package de.weinschenk.starlink.client.audio;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.lwjgl.PointerBuffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Client-seitiger Radio-Stream-Player.
 * Nutzt LWJGL STBVorbis (in Minecraft enthalten) für OGG/Vorbis-Dekodierung.
 * Decoded PCM wird über javax.sound.sampled.SourceDataLine ausgegeben.
 */
public class RadioStreamPlayer {

    public static final RadioStreamPlayer INSTANCE = new RadioStreamPlayer();
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int BUF_SIZE = 65536; // 64 KB Eingangspuffer

    private volatile boolean running = false;
    private Thread streamThread;
    private SourceDataLine audioLine;

    private RadioStreamPlayer() {}

    public void start(String streamUrl) {
        stop();
        running = true;
        streamThread = new Thread(() -> playStream(streamUrl), "starlink-radio-stream");
        streamThread.setDaemon(true);
        streamThread.start();
        LOGGER.info("Starlink: Radio stream starting -> {}", streamUrl);
    }

    public void stop() {
        running = false;
        closeAudioLine();
        if (streamThread != null && streamThread.isAlive()) {
            streamThread.interrupt();
            streamThread = null;
        }
        LOGGER.info("Starlink: Radio stream stopped.");
    }

    private synchronized void closeAudioLine() {
        SourceDataLine line = audioLine;
        audioLine = null;
        if (line != null) {
            line.stop();
            line.close();
        }
    }

    public boolean isPlaying() { return running; }

    /** Liefert das kombinierte Lautstärke-Produkt aus Master- und Records-Regler (0.0–1.0). */
    private static float getMcVolume() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return 1.0f;
        float master  = mc.options.getSoundSourceVolume(SoundSource.MASTER);
        float records = mc.options.getSoundSourceVolume(SoundSource.RECORDS);
        return master * records;
    }

    // -------------------------------------------------------------------------

    private void playStream(String streamUrl) {
        HttpURLConnection connection = null;
        long vorbis = 0;
        STBVorbisInfo vorbisInfo = STBVorbisInfo.malloc();
        ByteBuffer inputBuf = MemoryUtil.memAlloc(BUF_SIZE);
        inputBuf.limit(0); // start empty, read mode

        try {
            connection = openConnection(streamUrl);
            InputStream stream = connection.getInputStream();

            // Puffer initial füllen
            refill(stream, inputBuf);

            // STBVorbis im Push-Modus öffnen
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer usedBuf  = stack.mallocInt(1);
                IntBuffer errorBuf = stack.mallocInt(1);

                vorbis = STBVorbis.stb_vorbis_open_pushdata(inputBuf, usedBuf, errorBuf, null);
                if (vorbis == 0) {
                    LOGGER.error("Starlink: STBVorbis open failed, code={}", errorBuf.get(0));
                    return;
                }
                inputBuf.position(inputBuf.position() + usedBuf.get(0));
            }

            // Stream-Info lesen
            STBVorbis.stb_vorbis_get_info(vorbis, vorbisInfo);
            int sampleRate = vorbisInfo.sample_rate();
            int channels   = vorbisInfo.channels();
            LOGGER.info("Starlink: OGG stream opened: {}Hz, {} ch", sampleRate, channels);

            // PCM-Ausgabe öffnen
            AudioFormat format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16,
                    channels, channels * 2, sampleRate, false);
            audioLine = (SourceDataLine) AudioSystem.getLine(
                    new DataLine.Info(SourceDataLine.class, format));
            audioLine.open(format);
            audioLine.start();

            // Dekodierungs-Loop
            while (running) {
                if (inputBuf.remaining() < 4096) refill(stream, inputBuf);

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer    channelsBuf = stack.mallocInt(1);
                    IntBuffer    samplesBuf  = stack.mallocInt(1);
                    PointerBuffer outputPtr  = stack.mallocPointer(1);

                    int consumed = STBVorbis.stb_vorbis_decode_frame_pushdata(
                            vorbis, inputBuf, channelsBuf, outputPtr, samplesBuf);

                    if (consumed == 0) {
                        // Braucht mehr Daten
                        refill(stream, inputBuf);
                        continue;
                    }
                    inputBuf.position(inputBuf.position() + consumed);

                    int numSamples  = samplesBuf.get(0);
                    if (numSamples == 0) continue;

                    int numChannels    = channelsBuf.get(0);
                    long channelArray  = outputPtr.get(0); // float**

                    // float[] → int16 PCM interleaved, skaliert nach MC-Lautstärke
                    float volume = getMcVolume();
                    byte[] pcm = new byte[numSamples * numChannels * 2];
                    for (int i = 0; i < numSamples; i++) {
                        for (int c = 0; c < numChannels; c++) {
                            long chPtr = MemoryUtil.memGetAddress(channelArray + (long) c * 8);
                            float v = MemoryUtil.memGetFloat(chPtr + (long) i * 4);
                            int s = Math.max(-32768, Math.min(32767, (int)(v * volume * 32767.0f)));
                            int idx = (i * numChannels + c) * 2;
                            pcm[idx]     = (byte)(s & 0xFF);
                            pcm[idx + 1] = (byte)((s >> 8) & 0xFF);
                        }
                    }
                    audioLine.write(pcm, 0, pcm.length);
                }
            }

        } catch (IOException e) {
            if (running) LOGGER.error("Starlink: Stream IO error: {}", e.getMessage());
        } catch (LineUnavailableException e) {
            LOGGER.error("Starlink: Audio line unavailable: {}", e.getMessage());
        } finally {
            if (vorbis != 0) STBVorbis.stb_vorbis_close(vorbis);
            MemoryUtil.memFree(inputBuf);
            vorbisInfo.free();
            closeAudioLine();
            if (connection != null) connection.disconnect();
            running = false;
        }
    }

    /**
     * Kompaktiert den Puffer und liest neue Daten nach.
     * Invariante: Puffer ist vor und nach dem Aufruf im Read-Modus (position..limit = lesbare Daten).
     */
    private static void refill(InputStream stream, ByteBuffer buf) throws IOException {
        buf.compact(); // verbleibende Daten nach vorne, write-mode
        byte[] tmp = new byte[Math.min(buf.remaining(), 4096)];
        int n = stream.read(tmp);
        if (n == -1) throw new IOException("Stream ended");
        buf.put(tmp, 0, n);
        buf.flip(); // back to read mode
    }

    private static HttpURLConnection openConnection(String streamUrl) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(streamUrl).openConnection();
        c.setRequestProperty("User-Agent", "StarlinkMod/1.0 Minecraft");
        c.setRequestProperty("Accept", "audio/ogg");
        c.setRequestProperty("Icy-MetaData", "0");
        c.setConnectTimeout(5000);
        c.setReadTimeout(0);
        c.connect();
        int code = c.getResponseCode();
        if (code < 200 || code >= 300) throw new IOException("HTTP " + code);
        return c;
    }
}

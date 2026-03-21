package de.weinschenk.starlink.data;

/**
 * Filter-Modus für Satellitensignale.
 * Wird sowohl von ReceiverBlockEntity (Server) als auch von GlassesFilterMode (Client) verwendet.
 */
public enum SignalFilterMode {
    ALL,           // alle Satelliten
    PUBLIC_ONLY,   // nur öffentliche
    PRIVATE_ONLY;  // nur private (PIN-Abgleich beim Block, visuell beim Glas)

    public SignalFilterMode next() {
        return values()[(ordinal() + 1) % 3];
    }

    public String displayName() {
        return switch (this) {
            case ALL          -> "Alle";
            case PUBLIC_ONLY  -> "Öffentlich";
            case PRIVATE_ONLY -> "Privat";
        };
    }
}

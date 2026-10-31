package dev.color.nametag;

/** The prefix and suffix of a fake team. Targets that should look the same share one team. */
public final class TeamStyle {

    private final String prefix;
    private final String suffix;

    public TeamStyle(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String prefix() {
        return prefix;
    }

    public String suffix() {
        return suffix;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TeamStyle)) return false;
        TeamStyle other = (TeamStyle) o;
        return prefix.equals(other.prefix) && suffix.equals(other.suffix);
    }

    @Override
    public int hashCode() {
        return 31 * prefix.hashCode() + suffix.hashCode();
    }
}

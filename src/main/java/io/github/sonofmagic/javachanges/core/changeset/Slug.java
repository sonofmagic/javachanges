package io.github.sonofmagic.javachanges.core.changeset;

final class Slug {
    private Slug() {
    }

    static String slugify(String text) {
        StringBuilder builder = new StringBuilder();
        char previous = '-';
        for (int i = 0; i < text.length(); i++) {
            char current = Character.toLowerCase(text.charAt(i));
            if ((current >= 'a' && current <= 'z') || (current >= '0' && current <= '9')) {
                builder.append(current);
                previous = current;
            } else if (previous != '-') {
                builder.append('-');
                previous = '-';
            }
        }
        String slug = builder.toString();
        while (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        while (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        if (slug.isEmpty()) {
            return "changeset";
        }
        return slug.length() > 48 ? slug.substring(0, 48) : slug;
    }
}

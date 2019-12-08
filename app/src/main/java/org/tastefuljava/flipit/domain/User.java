package org.tastefuljava.flipit.domain;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class User {
    private static final Charset DIGEST_ENCODING = StandardCharsets.UTF_8;
    private static final String DIGEST_ALGORITHM = "SHA-256";

    private String email;
    private String displayName;
    private final List<Facet> facets = new ArrayList<>();

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<Facet> getFacets() {
        return new ArrayList<>(facets);
    }

    public Facet getFacet(int index) {
        if (index < 0 || index >= facets.size()) {
            return null;
        } else {
            return facets.get(index);
        }
    }

    public void addFacet(Facet facet) {
        facets.add(facet);
    }

    public void clearFacets() {
        facets.clear();
    }
}

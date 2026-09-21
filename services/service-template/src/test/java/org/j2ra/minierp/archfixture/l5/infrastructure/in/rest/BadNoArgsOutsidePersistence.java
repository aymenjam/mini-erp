package org.j2ra.minierp.archfixture.l5.infrastructure.in.rest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// The right form, but in the wrong package.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BadNoArgsOutsidePersistence {
    private String name;
}

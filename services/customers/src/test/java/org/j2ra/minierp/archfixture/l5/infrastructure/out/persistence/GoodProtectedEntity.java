package org.j2ra.minierp.archfixture.l5.infrastructure.out.persistence;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** The allowed form. Rule L5 must not report it. */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodProtectedEntity {
    private String name;
}

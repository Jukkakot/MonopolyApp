package org.example.types;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TurnResult {
    Object nextSpotCriteria;
    PathMode pathMode;
    boolean shouldGoToJail;
}

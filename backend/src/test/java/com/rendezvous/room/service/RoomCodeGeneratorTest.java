package com.rendezvous.room.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoomCodeGeneratorTest {

    @Test
    void codesHaveSixCharactersFromUnambiguousAlphabet() {
        RoomCodeGenerator generator = new RoomCodeGenerator();

        for (int i = 0; i < 1000; i++) {
            String code = generator.generate();
            assertThat(code).hasSize(RoomCodeGenerator.LENGTH);
            assertThat(code.chars()).allMatch(c -> RoomCodeGenerator.ALPHABET.indexOf(c) >= 0);
            assertThat(code).doesNotContainPattern("[ILO01]");
        }
    }
}

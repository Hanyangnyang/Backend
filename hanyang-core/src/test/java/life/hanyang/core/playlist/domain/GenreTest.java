package life.hanyang.core.playlist.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenreTest {

    @Test
    void supportsOstGenre() {
        assertThat(Genre.OST.getDescription()).isEqualTo("OST");
    }
}

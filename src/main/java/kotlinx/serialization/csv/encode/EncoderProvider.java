package kotlinx.serialization.csv.encode;

import kotlinx.serialization.csv.Csv;
import kotlinx.serialization.encoding.Encoder;

import java.io.Writer;

/**
 * Hack to get access to internal Kotlin class RootCsvEncoder
 */
public class EncoderProvider {
    @SuppressWarnings("KotlinInternalInJava")
    public static Encoder getEncoder(Csv csv, Writer writer) {
        return new RootCsvEncoder(csv, writer);
    }
}

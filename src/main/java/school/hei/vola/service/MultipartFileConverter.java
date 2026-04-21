package school.hei.vola.service;

import static java.io.File.createTempFile;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MultipartFileConverter implements Function<MultipartFile, File> {
  @Override
  public File apply(MultipartFile multipartFile) {
    try {
      var tempFile =
          createTempFile(Objects.requireNonNull(multipartFile.getOriginalFilename()), null);
      multipartFile.transferTo(tempFile);
      return tempFile;
    } catch (IOException e) {
      throw new RuntimeException("Server exception : " + e);
    }
  }
}

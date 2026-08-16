package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.entity.DocCenterDocument;
import java.io.IOException;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/** 文档中心文件存取：磁盘存储 + 数据库记录。 */
public interface DocCenterStorageService {
  /** 校验并保存上传文件，返回入库实体（未写库）。 */
  DocCenterDocument store(MultipartFile file) throws IOException;

  /** 覆盖保存 Document Server 回调产生的新版本内容。 */
  void overwrite(DocCenterDocument document, byte[] content) throws IOException;

  /** 读取文件内容流。 */
  Optional<Resource> load(DocCenterDocument document);

  /** 删除磁盘文件。 */
  void delete(DocCenterDocument document);
}

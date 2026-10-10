package com.cp.friend.service;
import com.cp.friend.service.strategy.ImageContentStrategy;
import com.cp.friend.port.StoragePort;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ImageContentStrategyTest {
 StoragePort storage=mock(StoragePort.class);ImageContentStrategy strategy=new ImageContentStrategy(storage);
 @Test void refusesNonImageBeforeStorage(){assertThrows(ResponseStatusException.class,()->strategy.process("data:image/png;base64,YWJj"));verifyNoInteractions(storage);}
 @Test void refusesSvgBeforeStorage(){assertThrows(ResponseStatusException.class,()->strategy.process("data:image/svg+xml;base64,YWJj"));verifyNoInteractions(storage);}
 @Test void savesRealImageUrl()throws Exception{var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);String data="data:image/png;base64,"+java.util.Base64.getEncoder().encodeToString(bytes.toByteArray());when(storage.uploadBase64(data)).thenReturn("photo.png");when(storage.publicUrl("photo.png")).thenReturn("https://images.test/photo.png");assertEquals("https://images.test/photo.png",strategy.process(data));}
}

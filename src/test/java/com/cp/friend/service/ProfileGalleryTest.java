package com.cp.friend.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cp.friend.model.User;
import com.cp.friend.repository.UserRepository;
import com.cp.friend.port.StoragePort;
import com.cp.friend.dto.request.UpdateProfileRequest;
import com.cp.friend.dto.request.UpdateProfileRequest.GalleryPhoto;
import com.cp.friend.dto.response.UpdateProfileResponse;
import java.util.*;
import org.springframework.web.server.ResponseStatusException;
class ProfileGalleryTest {
 final UserRepository repository=mock(UserRepository.class);final StoragePort storage=mock(StoragePort.class);final UserInterestService interests=mock(UserInterestService.class);final UUID id=UUID.randomUUID();final User user=new User();final UserService service=new UserService(repository,storage,interests);
 ProfileGalleryTest(){when(repository.findById(id)).thenReturn(Optional.of(user));when(repository.save(user)).thenReturn(user);user.getGalleryPhotos().addAll(List.of("https://images.test/a.jpg","https://images.test/b.jpg"));}
 @Test void omittedGalleryPreservesExistingPhotos(){service.updateProfile(id,new UpdateProfileRequest());assertEquals(2,user.getGalleryPhotos().size());verifyNoInteractions(storage,interests);}
 @Test void emptyGalleryClearsAndAllowsAvatarFallback(){var r=new UpdateProfileRequest();r.setGalleryPhotos(List.of());service.updateProfile(id,r);assertTrue(user.getGalleryPhotos().isEmpty());}
 @Test void retainedPhotosCanBeReordered(){var r=new UpdateProfileRequest();r.setGalleryPhotos(List.of(new GalleryPhoto("https://images.test/b.jpg",null),new GalleryPhoto("https://images.test/a.jpg",null)));service.updateProfile(id,r);assertEquals("https://images.test/b.jpg",user.getGalleryPhotos().getFirst());verifyNoInteractions(storage);}
 @Test void anotherUsersPhotoUrlIsRejected(){var r=new UpdateProfileRequest();r.setGalleryPhotos(List.of(new GalleryPhoto("https://images.test/other.jpg",null)));assertThrows(ResponseStatusException.class,()->service.updateProfile(id,r));verifyNoInteractions(storage);}
 @Test void moreThanFivePhotosIsRejected(){var r=new UpdateProfileRequest();r.setGalleryPhotos(Collections.nCopies(6,new GalleryPhoto("https://images.test/a.jpg",null)));assertThrows(ResponseStatusException.class,()->service.updateProfile(id,r));}
 @Test void disguisedImageIsRejectedBeforeUpload(){var r=new UpdateProfileRequest();r.setGalleryPhotos(List.of(new GalleryPhoto(null,"data:image/png;base64,YWJj")));assertThrows(ResponseStatusException.class,()->service.updateProfile(id,r));verifyNoInteractions(storage);}
 @Test void realImageIsUploadedAndIncludedInProfileResponse()throws Exception{var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);var r=new UpdateProfileRequest();r.setGalleryPhotos(List.of(new GalleryPhoto(null,"data:image/png;base64,"+Base64.getEncoder().encodeToString(bytes.toByteArray()))));when(storage.uploadBase64(anyString())).thenReturn("new.png");when(storage.publicUrl("new.png")).thenReturn("https://images.test/new.png");service.updateProfile(id,r);assertEquals(List.of("https://images.test/new.png"),new UpdateProfileResponse(user).getGalleryPhotos());}
 @Test void interestSelectionIsUpdatedWithProfile(){var r=new UpdateProfileRequest();r.setInterestIds(List.of());r.setBio("  About me  ");service.updateProfile(id,r);verify(interests).replaceInterests(id,List.of());assertEquals("About me",user.getBio());}
 @Test void validAndEmptyPhotoSourcesCannotBeMixed(){var r=new UpdateProfileRequest();r.setGalleryPhotos(List.of(new GalleryPhoto(null,null)));assertThrows(ResponseStatusException.class,()->service.updateProfile(id,r));}
}

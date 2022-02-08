package com.sephrael.issuetrackingsystem.controller;

import com.sephrael.issuetrackingsystem.entity.File;
import com.sephrael.issuetrackingsystem.entity.User;
import com.sephrael.issuetrackingsystem.message.ResponseFile;
import com.sephrael.issuetrackingsystem.repository.FileRepository;
import com.sephrael.issuetrackingsystem.repository.UserRepository;
import com.sephrael.issuetrackingsystem.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileService fileService;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getFileById(@PathVariable long id, Principal principal) {
        File file = fileService.getFile(id);
        User currentUser = userRepository.findByEmail(principal.getName());

        // if the requested file is a Profile Picture AND if it matches the Current User's Organization, redirect to NOT FOUND page
        if(file.isProfilePicture() && file.getUser().getOrganization() != currentUser.getOrganization())
            return ResponseEntity.notFound().build();

        // if the requested file is NOT a Profile Picture AND if the Current User is NOT involved with the
        // File's associated 'Project', redirect to NOT FOUND page
        if(!file.isProfilePicture() && !file.getIssue().getProject().getUsers().contains(currentUser))
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(file.getData());
    }

    @GetMapping("/{userId}/{isProfilePicture}")
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable("userId") long userId, Principal principal,
                                                    @PathVariable("isProfilePicture") boolean isProfilePicture) {
        // if the requested File's 'Organization' does NOT match the Current User's 'Organization', redirect to NOT FOUND page
        if(userRepository.findUserById(userId).getOrganization() != userRepository.findByEmail(principal.getName()).getOrganization())
            return ResponseEntity.notFound().build();

        File file = fileRepository.findByUserAndIsProfilePicture(userRepository.findUserById(userId), isProfilePicture);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(file.getData());
    }

    @PostMapping("/upload/{userId}/{isProfilePicture}")
    public String uploadFile(@PathVariable("userId") long userId, @RequestParam("file")MultipartFile file, Principal principal,
                                                      @PathVariable("isProfilePicture") boolean isProfilePicture) {
        User user = userRepository.findUserById(userId);

        // if Current User does NOT match the requested User, redirect to 404 page
        if(userRepository.findByEmail(principal.getName()).getId() != userId)
            return "/error/404";

        // if the 'User' uploaded an empty 'File' AND the 'User' already has a 'Profile Picture', DELETE the User's current
        // 'Profile Picture' and SET the User's 'hasProfilePicture' to FALSE
        if(file.isEmpty() && user.hasProfilePicture()) {
            fileService.deleteFile(fileRepository.findByUserAndIsProfilePicture(user, isProfilePicture).getId());

            user.setHasProfilePicture(false);
            userRepository.save(user);

        // if 'File' is empty and 'User' does NOT have a 'Profile Picture', redirect to 'Account Profile' page
        } else if(file.isEmpty() && !user.hasProfilePicture()) {
            return "redirect:/account/profile/" + userId;

        // if 'File' is NOT empty AND 'User' does NOT have a 'Profile Picture', upload and set the 'File' to the User's
        // 'Profile Picture'
        } else if(!file.isEmpty() && !user.hasProfilePicture()) {
            fileService.uploadFile(user, file, isProfilePicture);
        } else
            return "/error/400";

        return "redirect:/account/profile/" + userId;
    }

    @RequestMapping("/{identifier}/{issueKey}/delete/{id}")
    public String deleteFile(@PathVariable("id") long id, @PathVariable("identifier") String identifier,
                             @PathVariable("issueKey") String issueKey, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "/organization/select-organization";

        // if Current User does NOT match the requested File's original Uploader, redirect to 404 page
        if(!Objects.equals(currentUser.getId(), fileService.getFile(id).getUser().getId()))
            return "/error/404";

        fileRepository.deleteById(id);

        return String.format("redirect:/issues/%s/view/%s", identifier, issueKey);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ResponseFile>> getAllFilesJson() {
        List<ResponseFile> files = fileService.getAllFiles().map(file -> {
            String fileDownloadUri = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/files/")
                    .path(file.getId().toString())
                    .toUriString();

            return new ResponseFile(
                    file.getName(),
                    fileDownloadUri,
                    file.getType(),
                    file.getData().length);
        }).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(files);
    }
}
package com.sephrael.issuetrackingsystem.controller;

import com.sephrael.issuetrackingsystem.entity.File;
import com.sephrael.issuetrackingsystem.entity.User;
import com.sephrael.issuetrackingsystem.repository.UserRepository;
import com.sephrael.issuetrackingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Objects;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @RequestMapping("/profile/{id}")
    public String showAccountProfilePage(@PathVariable("id") long id, Principal principal, Model model) {
        User currentUser = userRepository.findByEmail(principal.getName());

        // checks if the the requested Account Profile page matches the Current User
        if(currentUser.getId() != id)
            return "/error/404";

        model.addAttribute("user", userRepository.getById(id));
        model.addAttribute("file", new File());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return "/account-settings/account-profile";
    }

    @PostMapping("/profile/{id}/save")
    public String saveAccountProfileChanges(@PathVariable("id") long id, User user, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getId() != id)
            return "/error/404";

        User userToBeUpdated = userRepository.findUserById(id);

        userToBeUpdated.setFirstName(user.getFirstName());
        userToBeUpdated.setLastName(user.getLastName());
        userToBeUpdated.setEmail(user.getEmail());

        userRepository.save(userToBeUpdated);

        return "redirect:/account/profile/" + id;
    }

    @RequestMapping("/security/{id}")
    public String showAccountSecurityPage(@PathVariable("id") long id, Principal principal, Model model) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getId() != id)
            return "/error/404";

        model.addAttribute("user", userRepository.getById(id));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return "/account-settings/account-security";
    }

    @PostMapping("/security/{id}/save")
    public String saveAccountSecurityChanges(@PathVariable("id") long id, RedirectAttributes redirectAttributes,
                                             @RequestParam("currentPassword") String currentPassword,
                                             @RequestParam("newPassword") String newPassword,
                                             @RequestParam("confirmPassword") String confirmPassword, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getId() != id)
            return "/error/404";

        User userToBeUpdated = userRepository.findUserById(id);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // checks if the Current Password actually matches the User's current password and if the new passwords match
        if(passwordEncoder.matches(currentPassword, userToBeUpdated.getPassword()) && Objects.equals(newPassword, confirmPassword)) {
            String encodedPassword = passwordEncoder.encode(newPassword);
            userToBeUpdated.setPassword(encodedPassword);

            userRepository.save(userToBeUpdated);
            // popup alerts are displayed accordingly
            redirectAttributes.addFlashAttribute("passwordChangeSuccess", "Your password has been successfully changed!");
        } else if(!passwordEncoder.matches(currentPassword, userToBeUpdated.getPassword())) {
            redirectAttributes.addFlashAttribute("incorrectOldPassword", "Your current password does not match!");
        } else if(!Objects.equals(newPassword, confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordsDoNotMatch", "Passwords do not match. Please try again.");
        } else {
            redirectAttributes.addFlashAttribute("error", "An error has occurred. Please try again.");
        }

        return "redirect:/account/security/" + id;
    }

    @PostMapping("/security/{id}/delete")
    public String deleteAccount(@PathVariable(name = "id") long id, Principal principal, RedirectAttributes redirectAttributes,
                                @RequestParam(value = "password", required = false) String password) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getId() != id)
            return "/error/404";

        User userToBeDeleted = userRepository.findUserById(id);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // checks if the Password actually matches the User's current password
        if(passwordEncoder.matches(password, userToBeDeleted.getPassword())) {
            // unassigns a User from all Issues that they were previously assigned to
            userService.unassignAllIssuesBeforeUserDeletion(userToBeDeleted);

            // if a User is involved with any Projects, this removes them from all those Projects before deleting the User
            if(userToBeDeleted.getProjects() != null) {
                userService.removeUserFromAllProjects(userToBeDeleted);
            }

            userRepository.deleteById(id);
            // popup alerts are displayed accordingly
            redirectAttributes.addFlashAttribute("accountDeletionSuccess", "Your account has been successfully deleted!");
        } else {
            redirectAttributes.addFlashAttribute("accountDeletionFailed", "Your password does not match is on file!");
            return "redirect:/account/security/" + id;
        }

        return "redirect:/login";
    }

    @RequestMapping("/notifications/{id}")
    public String showAccountNotificationsPage(@PathVariable("id") long id, Principal principal, Model model) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getId() != id)
            return "/error/404";

        model.addAttribute("user", userRepository.getById(id));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return "/account-settings/account-notifications";
    }
}
package com.sephrael.issueoverflow.controller;

import com.sephrael.issueoverflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.security.Principal;

@Controller
public class WebController {
    @Autowired
    private UserRepository userRepository;

    @RequestMapping(value = "/multi-tenant-add-users.html", method = RequestMethod.GET)
    public String multiTenantAddUsers() {return "future-features/multi-tenant-add-users";}

    @GetMapping("/user-management-add-user.html")
    public String userManagementAddUser(Model model, Principal principal) {
        model.addAttribute("currentUser", userRepository.findByEmail(principal.getName()));
        model.addAttribute("currentUserProjects", userRepository.findByEmail(principal.getName()).getProjects());

        return "future-features/user-management-add-user";
    }
}

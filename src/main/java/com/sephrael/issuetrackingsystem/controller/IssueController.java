package com.sephrael.issuetrackingsystem.controller;

import com.sephrael.issuetrackingsystem.entity.*;
import com.sephrael.issuetrackingsystem.repository.*;
import com.sephrael.issuetrackingsystem.service.FileService;
import com.sephrael.issuetrackingsystem.service.IssueService;
import com.sephrael.issuetrackingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/issues")
public class IssueController {

    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private IssueService issueService;
    @Autowired
    private IssueKeySequenceRepository issueKeySequenceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private FileService fileService;

    @GetMapping("/all")
    public String showIssuesByOrganization(Principal principal, Model model) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "/organization/select-organization";

        List<Issue> issuesByOrganization = issueRepository.findByOrganization(currentUser.getOrganization());

        model.addAttribute("issues", issuesByOrganization);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return("/issues/issues-by-organization");
    }
    
    @GetMapping("/all/results")
    public String filterAllIssues(Model model, Principal principal,
                                  @RequestParam(value = "type", required = false) String type,
                                  @RequestParam(value = "status", required = false) String status,
                                  @RequestParam(value = "priority", required = false) String priority,
                                  @RequestParam(value = "createdBy", required = false) String createdBy,
                                  @RequestParam(value = "assignedTo", required = false) String assignedTo,
                                  @RequestParam(value = "projectIdentifier", required = false) String projectIdentifier) {

        User currentUser = userRepository.findByEmail(principal.getName());
        List<Issue> filteredIssues;

        if(currentUser.getOrganization() == null)
            return "/organization/select-organization";

        // this allows filter fields to be empty
        type = issueService.setEmptyFilterFieldToNull(type);
        status = issueService.setEmptyFilterFieldToNull(status);
        priority = issueService.setEmptyFilterFieldToNull(priority);
        createdBy = issueService.setEmptyFilterFieldToNull(createdBy);
        projectIdentifier = issueService.setEmptyFilterFieldToNull(projectIdentifier);

        // this allows a User to view Issues that are unassigned or assigned to a specific User or all issues that are
        // neither assigned nor unassigned
        if(Objects.equals(assignedTo, "Unassigned")) {
            filteredIssues = issueRepository.findByProjectAndStatusAndPriorityAndTypeAndUserAndAssignedTo(
                    projectRepository.findByIdentifierAndOrganization(projectIdentifier, currentUser.getOrganization()),
                    status, priority, type, userRepository.findByEmail(createdBy), null);
        } else {
            filteredIssues = issueRepository.findByProjectAndStatusAndPriorityAndTypeAndAssignedToAndUser(
                    projectRepository.findByIdentifierAndOrganization(projectIdentifier, currentUser.getOrganization()),
                    status, priority, type, userRepository.findByEmail(assignedTo), userRepository.findByEmail(createdBy));
        }

        model.addAttribute("issues", filteredIssues);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return ("/issues/issues-by-organization");
    }

    @GetMapping("/{identifier}")
    public String showIssuesByProject(@PathVariable("identifier") String identifier, Model model, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "/organization/select-organization";

        Project currentProject = projectRepository.findByIdentifierAndOrganization(identifier, currentOrganization);

        model.addAttribute("issues", currentProject.getIssues());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentProject", currentProject);
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return("/issues/issues-by-project");
    }

    @GetMapping("/{identifier}/results")
    public String filterIssues(@RequestParam(value = "type", required = false) String type,
                               @RequestParam(value = "status", required = false) String status,
                               @RequestParam(value = "priority", required = false) String priority,
                               @RequestParam(value = "createdBy", required = false) String createdBy,
                               @RequestParam(value = "assignedTo", required = false) String assignedTo,
                               @PathVariable("identifier") String identifier, Model model, Principal principal) {

        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "/organization/select-organization";

        Project currentProject = projectRepository.findByIdentifierAndOrganization(identifier, currentOrganization);
        List<Issue> filteredIssues;

        // this allows filter fields to be empty
        type = issueService.setEmptyFilterFieldToNull(type);
        status = issueService.setEmptyFilterFieldToNull(status);
        priority = issueService.setEmptyFilterFieldToNull(priority);
        createdBy = issueService.setEmptyFilterFieldToNull(createdBy);
        
        // this allows a User to view Issues that are unassigned or assigned to a specific User or all issues that are
        // neither assigned nor unassigned
        if(Objects.equals(assignedTo, "Unassigned")) {
            filteredIssues = issueRepository.findByProjectAndStatusAndPriorityAndTypeAndUserAndAssignedTo(currentProject,
                    status, priority, type, userRepository.findByEmail(createdBy), null);
        } else {
            filteredIssues = issueRepository.findByProjectAndStatusAndPriorityAndTypeAndAssignedToAndUser(currentProject,
                    status, priority, type, userRepository.findByEmail(assignedTo), userRepository.findByEmail(createdBy));
        }

        model.addAttribute("issues", filteredIssues);
        model.addAttribute("currentProject", currentProject);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return ("/issues/issues-by-project");
    }

    @RequestMapping("/{identifier}/new")
    public String showNewIssuePage(@PathVariable(name = "identifier") String identifier, Model model, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "/organization/select-organization";

        Issue issue = new Issue();
        model.addAttribute("issue", issue);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentProject", projectRepository.findByIdentifierAndOrganization(identifier, currentOrganization));
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        // this was previously used to CONNECT an 'Issue' to the 'User' that created the 'Issue'
        //model.addAttribute("users", userService.listAll());

        return("/issues/create-issue");
    }

    @PostMapping(value = "/new")
    public String saveIssue(@ModelAttribute("issue") Issue issue, Principal principal,
                            @RequestParam(value = "files", required = false)MultipartFile[] files) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "/organization/select-organization";

        // sets the Issue Key
        issueService.setIssueKey(issue, issue.getProject());

        // connects the newly created Issue to the current User that created the Issue
        currentUser.addToIssue(issue);

        // connects the issue to the current user's Organization
        currentUser.getOrganization().addToIssue(issue);

        issueService.save(issue);

        // if 'Attach File(s)' Field is NOT empty, they are uploaded to the DB and are CONNECTED to the requested 'Issue'
        if(!files[0].isEmpty()) {
            for (MultipartFile file : files) {
                fileService.uploadIssueAttachments(currentUser, file, false, issue);
            }
        }

        return ("redirect:/issues/" + issue.getProject().getIdentifier() + "/view/" + issue.getIssueKey());
    }

    @PostMapping(value = "/update")
    public String updateIssue(@ModelAttribute("issue") Issue nextIssue, Principal principal,
                              @RequestParam(value = "files", required = false)MultipartFile[] files,
                              @RequestParam(value = "isAttachFileForm", required = false) boolean isAttachFileForm) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "/organization/select-organization";

        Issue previousIssue = issueRepository.findIssueById(nextIssue.getId());

        if(!isAttachFileForm) {
            previousIssue.setUpdatedBy(currentUser);
            previousIssue.setTitle(nextIssue.getTitle());
            previousIssue.setDescription(nextIssue.getDescription());
            previousIssue.setType(nextIssue.getType());
            previousIssue.setPriority(nextIssue.getPriority());
            previousIssue.setStatus(nextIssue.getStatus());
            issueService.setIssueKey(previousIssue, nextIssue.getProject());
            previousIssue.setAssignedTo(nextIssue.getAssignedTo());
            previousIssue.setProject(nextIssue.getProject());

            issueService.save(previousIssue);
        }

        // if 'Attach File(s)' Field is NOT null AND NOT empty, they are uploaded to the DB and are CONNECTED to the requested 'Issue'
        if(files != null && !files[0].isEmpty()) {
            for (MultipartFile file : files) {
                fileService.uploadIssueAttachments(currentUser, file, false, previousIssue);
            }
        }

        return ("redirect:/issues/" + previousIssue.getProject().getIdentifier() + "/view/" + previousIssue.getIssueKey());
    }

    @RequestMapping("/{identifier}/view/{issueKey}")
    public String showViewIssuePage(@PathVariable("issueKey") String issueKey, @PathVariable(name = "identifier") String identifier, Model model, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "/organization/select-organization";

        Issue issue = issueRepository.findByIssueKeyAndOrganization(issueKey, currentOrganization);
        model.addAttribute("newComment", new Comment());
        model.addAttribute("comments", issue.getComments());
        model.addAttribute("issue", issue);
        model.addAttribute("attachments", issue.getFiles());
        model.addAttribute("issueChangeHistoryList", issueService.getIssueChangeHistoryList(issue.getId()));
        model.addAttribute("findRevisions", issueRepository.findRevisions(issue.getId()));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentProject", projectRepository.findByIdentifierAndOrganization(identifier, currentOrganization));
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return "/issues/issue-details";
    }

    @GetMapping("/{identifier}/edit/{issueKey}")
    public String showEditIssuePage(@PathVariable("issueKey") String issueKey, @PathVariable(name = "identifier") String identifier, Model model, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "/organization/select-organization";

        model.addAttribute("issue", issueRepository.findByIssueKeyAndOrganization(issueKey, currentOrganization));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentProject", projectRepository.findByIdentifierAndOrganization(identifier, currentOrganization));
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return "/issues/edit-issue";
    }

    @RequestMapping("/{identifier}/delete/{issueKey}/{isOrganizationList}")
    public String deleteIssue(@PathVariable(name = "issueKey") String issueKey, @PathVariable(name = "identifier") String identifier,
                              @PathVariable("isOrganizationList") boolean isOrganizationList, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentUser.getOrganization() == null)
            return "/organization/select-organization";

        Project currentProject = projectRepository.findByIdentifierAndOrganization(identifier, currentOrganization);

        issueService.delete(issueRepository.findByIssueKeyAndOrganization(issueKey, currentOrganization).getId());

        if(currentProject.getIssues().isEmpty()) {
            issueKeySequenceRepository.delete(issueKeySequenceRepository.findByProjectIdentifierAndProjectId(identifier, currentProject.getId()));
        }

        if(isOrganizationList)
            return "redirect:/issues/all";
        else
            return "redirect:/issues/" + identifier;
    }

    // this shows the json format of all the Issues
    @GetMapping(path = "/json")
    public @ResponseBody Iterable<Issue> getAllIssues() {
        return issueService.listAll();
    }

    // this shows the json format of all the Issues by Project
    @GetMapping(path = "/{identifier}/json")
    public @ResponseBody Iterable<Issue> getAllIssuesByProject(@PathVariable("identifier") String identifier, Principal principal) {
        return issueService.findProjectByIdentifierAndOrganization(identifier, userRepository.findByEmail(principal.getName()).getOrganization());
    }

    // this shows the json format of all the Comments
    @GetMapping(path = "/comments/json")
    public @ResponseBody Iterable<Comment> getAllComments() {
        return commentRepository.findAll();
    }
}
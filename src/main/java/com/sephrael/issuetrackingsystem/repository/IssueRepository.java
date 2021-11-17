package com.sephrael.issuetrackingsystem.repository;

import com.sephrael.issuetrackingsystem.entity.Issue;
import com.sephrael.issuetrackingsystem.entity.Project;
import com.sephrael.issuetrackingsystem.entity.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends CrudRepository<Issue, Long> {

    List<Issue> findByUser(User user);
    List<Issue> findByProject(Project project);
    Issue findIssueByTitle(String title);
//    @Query("select a from Issue a where a.issueId = ?1")
//    public Issue findIssue(Long issueId);

//    @Modifying
//    @Query("UPDATE Issue i SET i.user = User.id WHERE i.userEmail = User.email")
//    Long joinIssueAndUserByUserId(@Param(value = "id") long id, @Param(value = "email") String email);
}
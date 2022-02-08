package com.sephrael.issuetrackingsystem.repository;

import com.sephrael.issuetrackingsystem.entity.Organization;
import com.sephrael.issuetrackingsystem.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Project findByAccessKeyAndOrganization(String accessKey, Organization organization);
    Project findByIdentifierAndOrganization(String identifier, Organization organization);
    Project findProjectById(Long id);
    List<Project> findAllProjectsByOrganizationId(Long id);

    // was previously used to access Project information but this method caused issues
    //@Query("SELECT p FROM Project p WHERE p.identifier = ?1")
    //Project findByIdentifier(String identifier);

    // OLD CODE: was also previously used to access Project information but this method also caused issues
    //@Query("SELECT p FROM Project p WHERE p.accessKey = ?1")
    //Project findByAccessKey(String accessKey);
}

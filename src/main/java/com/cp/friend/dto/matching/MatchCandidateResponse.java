package com.cp.friend.dto.matching;

import java.util.Set;
import java.util.UUID;

/**
 * DTO for candidate recommended friends with calculated matching score.
 */
public class MatchCandidateResponse {
    private UUID userId;
    private String firstname;
    private String lastname;
    private String imageUrl;
    private String bio;
    private String department;
    private Short year;
    private Double matchScore;
    private Set<String> sharedInterests;

    public MatchCandidateResponse() {
    }

    public MatchCandidateResponse(UUID userId, String firstname, String lastname, String imageUrl,
                                  String bio, String department, Short year, Double matchScore,
                                  Set<String> sharedInterests) {
        this.userId = userId;
        this.firstname = firstname;
        this.lastname = lastname;
        this.imageUrl = imageUrl;
        this.bio = bio;
        this.department = department;
        this.year = year;
        this.matchScore = matchScore;
        this.sharedInterests = sharedInterests;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID userId;
        private String firstname;
        private String lastname;
        private String imageUrl;
        private String bio;
        private String department;
        private Short year;
        private Double matchScore;
        private Set<String> sharedInterests;

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder firstname(String firstname) {
            this.firstname = firstname;
            return this;
        }

        public Builder lastname(String lastname) {
            this.lastname = lastname;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder bio(String bio) {
            this.bio = bio;
            return this;
        }

        public Builder department(String department) {
            this.department = department;
            return this;
        }

        public Builder year(Short year) {
            this.year = year;
            return this;
        }

        public Builder matchScore(Double matchScore) {
            this.matchScore = matchScore;
            return this;
        }

        public Builder sharedInterests(Set<String> sharedInterests) {
            this.sharedInterests = sharedInterests;
            return this;
        }

        public MatchCandidateResponse build() {
            return new MatchCandidateResponse(userId, firstname, lastname, imageUrl, bio, department, year, matchScore, sharedInterests);
        }
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Short getYear() {
        return year;
    }

    public void setYear(Short year) {
        this.year = year;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }

    public Set<String> getSharedInterests() {
        return sharedInterests;
    }

    public void setSharedInterests(Set<String> sharedInterests) {
        this.sharedInterests = sharedInterests;
    }
}

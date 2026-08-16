package uk.ac.ebi.eva.submission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(schema = "eva_submissions", name = "submission_account", uniqueConstraints = {@UniqueConstraint(columnNames =
        {"user_id", "login_type"})})
public class SubmissionAccount {

    @Id
    private String id;

    @NonNull
    @Column(nullable = false, name = "user_id")
    private String userId;

    @NonNull
    @Column(nullable = false, name = "login_type")
    private String loginType;

    @NonNull
    @Column(nullable = false, name = "primary_email")
    private String primaryEmail;

    @ElementCollection
    @Column(name = "secondary_emails")
    private List<String> secondaryEmails;

    @NonNull
    @Column(name = "first_name")
    private String firstName;

    @NonNull
    @Column(name = "last_name")
    private String lastName;

    public SubmissionAccount() {

    }

    public SubmissionAccount(String userId, String loginType, String firstName, String lastName, String primaryEmail) {
        this.id = createId(userId, loginType);
        this.userId = userId;
        this.loginType = loginType;
        this.firstName = firstName != null ? firstName : "";
        this.lastName = lastName != null ? lastName : "";
        this.primaryEmail = primaryEmail;
        this.secondaryEmails = new ArrayList<>();
    }

    public SubmissionAccount(String userId, String loginType, String firstName, String lastName, String primaryEmail,
                             List<String> secondaryEmails) {
        this.id = createId(userId, loginType);
        this.userId = userId;
        this.loginType = loginType;
        this.firstName = firstName != null ? firstName : "";
        this.lastName = lastName != null ? lastName : "";
        this.primaryEmail = primaryEmail;
        this.secondaryEmails = secondaryEmails;
    }

    public String getId() {
        return id;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    @NonNull
    public String getLoginType() {
        return loginType;
    }

    @NonNull
    public String getPrimaryEmail() {
        return primaryEmail;
    }

    public void setPrimaryEmail(@NonNull String primaryEmail) {
        this.primaryEmail = primaryEmail;
    }

    public List<String> getSecondaryEmails() {
        return secondaryEmails;
    }

    public void setSecondaryEmails(List<String> secondaryEmails) {
        this.secondaryEmails = secondaryEmails;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName != null ? firstName : "";
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName != null ? lastName : "";
    }

    private String createId(String userId, String loginType) {
        return userId + "_" + loginType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmissionAccount)) return false;
        SubmissionAccount account = (SubmissionAccount) o;
        return Objects.equals(getId(), account.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
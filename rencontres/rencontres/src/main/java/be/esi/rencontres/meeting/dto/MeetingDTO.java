package be.esi.rencontres.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDTO {

    @NotBlank(message = "User ID 1 cannot be empty")
    private String userId1;

    @NotBlank(message = "User ID 2 cannot be empty")
    private String userId2;

    @NotNull(message = "Meeting date cannot be null")
    private LocalDateTime meetingDate;

    private String location;
}

package com.example.onlineshop.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTrackRequest {
    private String track_content;
    private String track_location;
    private String track_status;
}

package com.yearners.ua_parent_consent_backend.dto;

import com.yearners.ua_parent_consent_backend.model.Section;
import lombok.Data;

@Data
public class SectionResponse {

    private Long id;
    private String name;
    private Boolean active;

    public static SectionResponse from(Section section) {
        SectionResponse response = new SectionResponse();

        response.setId(section.getId());
        response.setName(section.getName());
        response.setActive(section.getActive());

        return response;
    }
}

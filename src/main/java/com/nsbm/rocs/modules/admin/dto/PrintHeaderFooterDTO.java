package com.nsbm.rocs.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrintHeaderFooterDTO {

    private HeaderDTO header;
    private FooterDTO footer;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeaderDTO {
        private String businessName;
        private String branchLine;
        private String address;
        private String contact;
        private String extraLine;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FooterDTO {
        private String thankYouLine;
        private String policyLine;
        private String poweredByLine;
        private String extraLine;
    }
}



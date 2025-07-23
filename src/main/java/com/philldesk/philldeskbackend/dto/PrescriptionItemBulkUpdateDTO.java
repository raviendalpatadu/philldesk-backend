package com.philldesk.philldeskbackend.dto;

import java.util.List;

public class PrescriptionItemBulkUpdateDTO {
    private List<PrescriptionItemDTO> items;

    public PrescriptionItemBulkUpdateDTO() {}

    public PrescriptionItemBulkUpdateDTO(List<PrescriptionItemDTO> items) {
        this.items = items;
    }

    public List<PrescriptionItemDTO> getItems() {
        return items;
    }

    public void setItems(List<PrescriptionItemDTO> items) {
        this.items = items;
    }
}

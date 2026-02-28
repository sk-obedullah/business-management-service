package com.jewelry.controller;

import com.jewelry.dto.CustomerDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class CustomerViewController {

    @FXML
    private Label lblInitials;
    @FXML
    private Label lblFullName;
    @FXML
    private Label lblEmail;
    @FXML
    private Label lblPhone;
    @FXML
    private Label lblAddress;
    @FXML
    private Label lblNotes;

    private CustomerDTO currentDTO;
    private Consumer<CustomerDTO> onEditAction;

    public void initData(CustomerDTO dto) {
        this.currentDTO = dto;
        populateFields(dto);
    }

    public void setOnEditAction(Consumer<CustomerDTO> onEditAction) {
        this.onEditAction = onEditAction;
    }

    private void populateFields(CustomerDTO dto) {
        String fName = dto.firstName() != null ? dto.firstName().trim() : "";
        String lName = dto.lastName() != null ? dto.lastName().trim() : "";

        String fullName = fName + " " + lName;
        if (fullName.isBlank())
            fullName = "Unknown Customer";
        lblFullName.setText(fullName.trim());

        // Extract initials
        String initials = "";
        if (!fName.isEmpty())
            initials += fName.substring(0, 1).toUpperCase();
        if (!lName.isEmpty())
            initials += lName.substring(0, 1).toUpperCase();
        if (initials.isEmpty())
            initials = "?";
        lblInitials.setText(initials);

        lblEmail.setText(dto.email() != null && !dto.email().isBlank() ? dto.email() : "N/A");
        lblPhone.setText(dto.phone() != null && !dto.phone().isBlank() ? dto.phone() : "N/A");
        lblAddress.setText(
                dto.address() != null && !dto.address().isBlank() ? dto.address() : "No address on file.");
        lblNotes.setText(dto.notes() != null && !dto.notes().isBlank() ? dto.notes() : "No additional notes.");
    }

    @FXML
    private void onEdit() {
        if (onEditAction != null && currentDTO != null) {
            onEditAction.accept(currentDTO);
        }
    }

    @FXML
    private void onBack() {
        MainLayoutController.navigateTo("/fxml/customer/CustomerList.fxml", (CustomerListController controller) -> {
            // Can pass state back if necessary
        });
    }
}

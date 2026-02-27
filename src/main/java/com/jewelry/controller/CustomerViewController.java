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
        String fName = dto.getFirstName() != null ? dto.getFirstName().trim() : "";
        String lName = dto.getLastName() != null ? dto.getLastName().trim() : "";

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

        lblEmail.setText(dto.getEmail() != null && !dto.getEmail().isBlank() ? dto.getEmail() : "N/A");
        lblPhone.setText(dto.getPhone() != null && !dto.getPhone().isBlank() ? dto.getPhone() : "N/A");
        lblAddress.setText(
                dto.getAddress() != null && !dto.getAddress().isBlank() ? dto.getAddress() : "No address on file.");
        lblNotes.setText(dto.getNotes() != null && !dto.getNotes().isBlank() ? dto.getNotes() : "No additional notes.");
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

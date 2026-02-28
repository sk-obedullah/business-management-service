package com.jewelry.controller;

import com.jewelry.util.SnackbarUtil;
import com.jewelry.dto.CustomerDTO;
import com.jewelry.entity.Customer;
import com.jewelry.exception.AppException;
import com.jewelry.service.CustomerService;
import com.jewelry.util.CustomerMapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.Parent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Customer Add/Edit modal dialog (CustomerForm.fxml).
 *
 * <p>
 * Operates in two modes set by {@link #initForEdit(CustomerDTO)}:
 * <ul>
 * <li><strong>Add mode (dto == null)</strong> — creates a new Customer</li>
 * <li><strong>Edit mode (dto != null)</strong> — populates fields, updates on
 * save</li>
 * </ul>
 *
 * <p>
 * Email uniqueness and entity-existence validation is delegated entirely
 * to {@link CustomerService}. This controller only catches
 * {@link AppException} and surfaces it as a styled inline error label.
 */
@Component
public class CustomerFormController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CustomerFormController.class);

    @Autowired
    private CustomerService customerService;

    // ── FXML Bindings ────────────────────────────────────────────────────────
    @FXML
    private Label lblTitle;
    @FXML
    private TextField txtFirstName;
    @FXML
    private TextField txtLastName;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtPhone;
    @FXML
    private TextArea txtAddress;
    @FXML
    private TextArea txtNotes;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnCancel;
    @FXML
    private Label lblError;

    // ── State ────────────────────────────────────────────────────────────────
    private CustomerDTO currentDTO;
    private boolean saved = false;
    private Runnable closeAction = () -> MainLayoutController.navigateTo("/fxml/customer/CustomerList.fxml");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblError.setVisible(false);

        // Phone field: allow only digits, +, -, spaces
        txtPhone.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("[\\d\\+\\-\\s]*")) {
                txtPhone.setText(oldVal);
            }
        });
    }

    /**
     * Called by {@link CustomerListController} before the dialog is shown.
     *
     * @param dto {@code null} → Add mode; populated DTO → Edit mode
     */
    public void initForEdit(CustomerDTO dto) {
        this.currentDTO = dto;

        if (dto == null) {
            lblTitle.setText("Add New Customer");
            btnSave.setText("Save Customer");
        } else {
            lblTitle.setText("Edit Customer");
            btnSave.setText("Update Customer");
            populateFields(dto);
        }
    }

    public void setCloseAction(Runnable closeAction) {
        this.closeAction = closeAction;
    }

    // ── FXML Actions ─────────────────────────────────────────────────────────

    @FXML
    private void onSave() {
        lblError.setVisible(false);
        try {
            CustomerDTO dto = collectFormData();

            if (currentDTO == null) {
                // Add mode
                Customer created = customerService.createCustomer(CustomerMapper.toEntity(dto));
                log.info("Customer created via form: id={}", created.getId());
                SnackbarUtil.showSuccess(btnSave, "Customer added successfully!");
            } else {
                // Edit mode
                customerService.updateCustomer(CustomerMapper.toEntity(dto));
                log.info("Customer updated via form: id={}", dto.id());
                SnackbarUtil.showSuccess(btnSave, "Customer updated successfully!");
            }

            saved = true;
            closeDialog();

        } catch (AppException | IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void populateFields(CustomerDTO dto) {
        txtFirstName.setText(dto.firstName() != null ? dto.firstName() : "");
        txtLastName.setText(dto.lastName() != null ? dto.lastName() : "");
        txtEmail.setText(dto.email() != null ? dto.email() : "");
        txtPhone.setText(dto.phone() != null ? dto.phone() : "");
        txtAddress.setText(dto.address() != null ? dto.address() : "");
        txtNotes.setText(dto.notes() != null ? dto.notes() : "");
    }

    private String safeTrim(String str) {
        return str == null ? "" : str.trim();
    }

    private CustomerDTO collectFormData() {
        String firstName = safeTrim(txtFirstName.getText());
        String lastName = safeTrim(txtLastName.getText());
        String email = safeTrim(txtEmail.getText()).toLowerCase();
        String phone = safeTrim(txtPhone.getText());
        String address = safeTrim(txtAddress.getText());
        String notes = safeTrim(txtNotes.getText());

        return new CustomerDTO(
                currentDTO == null ? null : currentDTO.id(),
                firstName.isBlank() ? null : firstName,
                lastName.isBlank() ? null : lastName,
                email.isBlank() ? null : email,
                phone.isBlank() ? null : phone,
                address.isBlank() ? null : address,
                notes.isBlank() ? null : notes,
                currentDTO == null ? null : currentDTO.createdAt()
        );
    }

    private void showError(String message) {
        lblError.setText("⚠ " + message);
        lblError.setVisible(true);
    }

    private void closeDialog() {
        if (closeAction != null) {
            closeAction.run();
        }
    }

    /** Called by the parent list controller. */
    public boolean isSaved() {
        return saved;
    }
}

package com.jewelry.controller;

import com.jewelry.config.AppContext;
import com.jewelry.dto.CustomerDTO;
import com.jewelry.entity.Customer;
import com.jewelry.exception.AppException;
import com.jewelry.service.CustomerService;
import com.jewelry.util.CustomerMapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
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
public class CustomerFormController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CustomerFormController.class);

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
    private final CustomerService customerService;
    private CustomerDTO currentDTO;
    private boolean saved = false;
    private Runnable closeAction = () -> MainLayoutController.navigateTo("/fxml/customer/CustomerList.fxml");

    public CustomerFormController() {
        this.customerService = AppContext.getInstance().getCustomerService();
    }

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
            } else {
                // Edit mode
                dto.setId(currentDTO.getId());
                customerService.updateCustomer(CustomerMapper.toEntity(dto));
                log.info("Customer updated via form: id={}", dto.getId());
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
        txtFirstName.setText(dto.getFirstName() != null ? dto.getFirstName() : "");
        txtLastName.setText(dto.getLastName() != null ? dto.getLastName() : "");
        txtEmail.setText(dto.getEmail() != null ? dto.getEmail() : "");
        txtPhone.setText(dto.getPhone() != null ? dto.getPhone() : "");
        txtAddress.setText(dto.getAddress() != null ? dto.getAddress() : "");
        txtNotes.setText(dto.getNotes() != null ? dto.getNotes() : "");
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

        if (firstName.isBlank())
            throw new IllegalArgumentException("First name is required.");
        if (lastName.isBlank())
            throw new IllegalArgumentException("Last name is required.");
        if (phone.isBlank())
            throw new IllegalArgumentException("Phone number is required.");

        if (!email.isBlank() && !email.contains("@"))
            throw new IllegalArgumentException("Enter a valid email address.");

        CustomerDTO dto = new CustomerDTO();
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setEmail(email.isBlank() ? null : email);
        dto.setPhone(phone);
        dto.setAddress(address.isBlank() ? null : address);
        dto.setNotes(notes.isBlank() ? null : notes);
        return dto;
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

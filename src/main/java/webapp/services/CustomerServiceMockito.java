

package webapp.services;

import java.util.ArrayList;
import java.util.List;

import webapp.persistence.AddressRowDataGateway;
import webapp.persistence.CustomerRowDataGateway;
import webapp.persistence.PersistenceException;

public class CustomerServiceMockito {
	
	// Removi o modificador INSTANCE e os métodos de enumeração
		
	
    public CustomerDTO getCustomerByVat(int vat) throws ApplicationException {
        if (!isValidVAT(vat))
            throw new ApplicationException("Invalid VAT number: " + vat);
		else {
			CustomerRowDataGateway customer = new CustomerRowDataGateway(vat, "Mock Customer", 123456789);
			return new CustomerDTO(customer.getCustomerId(), customer.getVAT(), customer.getDesignation(),
			        customer.getPhoneNumber());
		}
    }

    public void addCustomer(int vat, String designation, int phoneNumber) throws ApplicationException {
        if (!isValidVAT(vat))
            throw new ApplicationException("Invalid VAT number: " + vat);
        else
            try {
                CustomerRowDataGateway customer = new CustomerRowDataGateway(vat, designation, phoneNumber);
                customer.insert();
            } catch (PersistenceException e) {
                throw new ApplicationException("Can't add customer with vat number " + vat + ".", e);
            }
    }

    public CustomersDTO getAllCustomers() throws ApplicationException {
        try {
            List<CustomerRowDataGateway> customers = new CustomerRowDataGateway().getAllCustomers();
            List<CustomerDTO> list = new ArrayList<CustomerDTO>();
            for (CustomerRowDataGateway cust : customers) {
                list.add(new CustomerDTO(cust.getCustomerId(), cust.getVAT(), cust.getDesignation(),
                        cust.getPhoneNumber()));
            }
            CustomersDTO c = new CustomersDTO(list);
            return c;
        } catch (PersistenceException e) {
            throw new ApplicationException("Error getting all customers", e);
        }
    }

    public void addAddressToCustomer(int customerVat, String addr) throws ApplicationException {
        if (!isValidVAT(customerVat))
            throw new ApplicationException("Invalid VAT number: " + customerVat);
        else
            try {
                AddressRowDataGateway address = new AddressRowDataGateway(addr, customerVat);
                address.insert();
            } catch (PersistenceException e) {
                throw new ApplicationException(
                        "Can't add the address /n" + addr + "/nTo customer with vat number " + customerVat + ".", e);
            }
    }

    public AddressesDTO getAllAddresses(int customerVat) throws ApplicationException {
        try {
            List<AddressRowDataGateway> addrs = new AddressRowDataGateway().getCustomerAddresses(customerVat);
            List<AddressDTO> list = new ArrayList<>();
            for (AddressRowDataGateway addr : addrs) {
                list.add(new AddressDTO(addr.getId(), addr.getCustVat(), addr.getAddress()));
            }
            AddressesDTO c = new AddressesDTO(list);
            return c;
        } catch (PersistenceException e) {
            throw new ApplicationException("Error getting all customers", e);
        }
    }

    public void updateCustomerPhone(int vat, int phoneNumber) throws ApplicationException {
        if (!isValidVAT(vat))
            throw new ApplicationException("Invalid VAT number: " + vat);
        else
            try {
                CustomerRowDataGateway customer = new CustomerRowDataGateway(vat, "Mock Customer", phoneNumber);
                customer.updatePhoneNumber();
            } catch (PersistenceException e) {
                throw new ApplicationException("Customer with vat number " + vat + " not found.", e);
            }
    }

    public void removeCustomer(int vat) throws ApplicationException {
        if (!isValidVAT(vat))
            throw new ApplicationException("Invalid VAT number: " + vat);
        else
            try {
                CustomerRowDataGateway customer = new CustomerRowDataGateway(vat, "Mock Customer", 123456789);
                customer.removeCustomer();
            } catch (PersistenceException e) {
                throw new ApplicationException("Customer with vat number " + vat + " doesn't exist.", e);
            }
    }

    private boolean isValidVAT(int vat) {
        if (vat < 100000000 || vat > 999999999)
            return false;

        int firstDigit = vat / 100000000;
        if (firstDigit != 1 && firstDigit != 2 && firstDigit != 5 && firstDigit != 6 && firstDigit != 8
                && firstDigit != 9)
            return false;

        int sum = 0;
        int checkDigit = vat % 10;
        vat /= 10;

        for (int i = 2; i < 10 && vat != 0; i++) {
            sum += vat % 10 * i;
            vat /= 10;
        }

        int checkDigitCalc = 11 - sum % 11;
        if (checkDigitCalc == 10)
            checkDigitCalc = 0;
		return checkDigit == checkDigitCalc;
	}
}

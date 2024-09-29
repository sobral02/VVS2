package DbSetup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static DbSetup.DBSetupUtils.DB_PASSWORD;
import static DbSetup.DBSetupUtils.DB_URL;
import static DbSetup.DBSetupUtils.DB_USERNAME;
import static DbSetup.DBSetupUtils.DELETE_ALL;
import static DbSetup.DBSetupUtils.INSERT_CUSTOMER_ADDRESS_DATA;
import static DbSetup.DBSetupUtils.startApplicationDatabaseForTesting;

import java.sql.SQLException;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.Operations;

import com.ninja_squad.dbsetup.destination.Destination;
import com.ninja_squad.dbsetup.destination.DriverManagerDestination;
import com.ninja_squad.dbsetup.operation.Operation;

import webapp.services.ApplicationException;
import webapp.services.CustomerDTO;
import webapp.services.CustomerService;

public class CustomersDBTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private static Destination dataSource;

	private static DbSetupTracker dbSetupTracker = new DbSetupTracker();

	@BeforeClass
	public static void setupClass() {
		startApplicationDatabaseForTesting();
		dataSource = DriverManagerDestination.with(DB_URL, DB_USERNAME, DB_PASSWORD);
	}

	@Before
	public void setup() throws SQLException {
		Operation initDBOperations = Operations.sequenceOf(DELETE_ALL, INSERT_CUSTOMER_ADDRESS_DATA);
		Operations.sequenceOf(DELETE_ALL, INSERT_CUSTOMER_ADDRESS_DATA);
		DbSetup dbSetup = new DbSetup(dataSource, initDBOperations);
		dbSetupTracker.launchIfNecessary(dbSetup);
	}

	/**
	 * Testa se o sistema não permite adicionar um novo cliente com um VAT
	 * existente. ponto a)
	 * 
	 * @throws ApplicationException
	 */
	@Test
	public void addCustomerWithExistingVAT() {
		int existingVAT = 197672337; // Use um VAT que já exista nos dados de teste

		// Verifica que o VAT já existe na DB
		CustomerDTO existingCustomer;
		try {
			existingCustomer = CustomerService.INSTANCE.getCustomerByVat(existingVAT);
			assertNotNull(existingCustomer);
		} catch (ApplicationException e) {
			fail("Failed to retrieve existing customer: " + e.getMessage());
			return; // Termina o teste se não conseguir obter o cliente existente
		}

		assertThrows(ApplicationException.class, () -> {
			CustomerService.INSTANCE.addCustomer(existingCustomer.vat, "Novo Cliente", 197672337);
		});

	}

	/**
	 * Teste que verifica que depois de fazer update das informações do customer,
	 * estas informaçoes são guardadas devidamente
	 * 
	 * ponto b)
	 * 
	 * @throws ApplicationException
	 */
	@Test
	public void checkCustomerUpdate() throws ApplicationException {
		int vat = 197672337; // vat de um cliente existente
		int phone = 961861761; // novo phonenumber a atualizar

		CustomerDTO customer = null;
		CustomerDTO otherCustomer = null;

		try {
			customer = CustomerService.INSTANCE.getCustomerByVat(vat);
			CustomerService.INSTANCE.updateCustomerPhone(customer.vat, phone);
			otherCustomer = CustomerService.INSTANCE.getCustomerByVat(customer.vat);

		} catch (ApplicationException e) {
			fail("Failed to retrieve update customer result: " + e.getMessage());
		}

		assertNotEquals(customer.phoneNumber, otherCustomer.phoneNumber);
		assertEquals(otherCustomer.phoneNumber, phone);
	}

	/**
	 * Teste que verifica que quando todos os clientes são removidos, então a lista
	 * de clientes passa a estar vazia
	 * 
	 * ponto c)
	 * 
	 * @throws ApplicationException
	 */

	@Test
	public void deleteAllButOneCustomer() throws ApplicationException {
		List<CustomerDTO> listOfCustomers = CustomerService.INSTANCE.getAllCustomers().customers;
		int size = listOfCustomers.size();
		assertNotEquals(0, size);

		try {
			for (int i = 0; i <= size - 1; i++) {
				CustomerService.INSTANCE.removeCustomer(listOfCustomers.get(i).vat);
			}
		} catch (ApplicationException e) {
			fail("Failed to delete all costumers: " + e.getMessage());
		}

		List<CustomerDTO> listOfCustomersAfter = CustomerService.INSTANCE.getAllCustomers().customers;
		int sizeAfter = listOfCustomersAfter.size();

		assertEquals(0, sizeAfter);

	}

	/**
	 * Teste que permite verificar que depois de remover um customer é possível
	 * inseri-lo de volta sem problemas
	 * 
	 * ponto d)
	 * 
	 * @throws ApplicationException
	 */
	@Test
	public void addDeletedCustomer() throws ApplicationException {
		int vat = 197672337;
		CustomerDTO customer = null;
		try {
			customer = CustomerService.INSTANCE.getCustomerByVat(vat);
		} catch (ApplicationException e) {
			fail("Erro o cleinet não existe " + e.getMessage());
		}

		assertNotNull(customer);

		// Remove o cliente
		try {

			CustomerService.INSTANCE.removeCustomer(vat);

			List<CustomerDTO> updatedListOfCustomers = CustomerService.INSTANCE.getAllCustomers().customers;

			// Verifica se um cliente com o VAT pretendido foi adicionado novamente
			boolean customerRemoved = true;
			for (CustomerDTO updatedCustomer : updatedListOfCustomers) {
				if (updatedCustomer.vat == vat) {
					customerRemoved = false;
					break;
				}
			}

			assertTrue(customerRemoved); // Verifica se o cliente foi removido novamente
		} catch (ApplicationException e) {
			fail("Erro ao tentar eliminar o cliente: " + e.getMessage());
		}

		// Adiciona o cliente novamente e verifica se ele foi adicionado corretamente
		try {
			CustomerService.INSTANCE.addCustomer(customer.vat, customer.designation, customer.phoneNumber);

			// Obtém a lista atualizada de clientes
			List<CustomerDTO> updatedListOfCustomers = CustomerService.INSTANCE.getAllCustomers().customers;

			// Verifica se um cliente com o VAT pretendido foi adicionado novamente
			boolean customerAddedAgain = false;
			for (CustomerDTO updatedCustomer : updatedListOfCustomers) {
				if (updatedCustomer.vat == vat) {
					customerAddedAgain = true;
					break;
				}
			}

			assertTrue(customerAddedAgain); // Verifica se o cliente foi adicionado novamente
		} catch (ApplicationException e) {
			fail("Erro ao adicionar cliente novamente: " + e.getMessage());
		}
	}

}

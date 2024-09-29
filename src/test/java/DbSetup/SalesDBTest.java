package DbSetup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static DbSetup.DBSetupUtils.DB_PASSWORD;
import static DbSetup.DBSetupUtils.DB_URL;
import static DbSetup.DBSetupUtils.DB_USERNAME;
import static DbSetup.DBSetupUtils.DELETE_ALL;
import static DbSetup.DBSetupUtils.INSERT_CUSTOMER_SALE_DATA;
import static DbSetup.DBSetupUtils.NUM_INIT_SALES;
import static DbSetup.DBSetupUtils.NUM_INIT_SALES_DELIVERIES;
import static DbSetup.DBSetupUtils.startApplicationDatabaseForTesting;

import java.sql.SQLException;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.Operations;

import com.ninja_squad.dbsetup.destination.Destination;
import com.ninja_squad.dbsetup.destination.DriverManagerDestination;
import com.ninja_squad.dbsetup.operation.Operation;

import webapp.persistence.PersistenceException;
import webapp.persistence.SaleStatus;
import webapp.services.ApplicationException;
import webapp.services.CustomerDTO;
import webapp.services.CustomerService;
import webapp.services.SaleDTO;
import webapp.services.SaleService;
import webapp.services.SalesDTO;
import webapp.services.SalesDeliveryDTO;

public class SalesDBTest {

	private static Destination dataSource;

	// the tracker is static because JUnit uses a separate Test instance for every
	// test method.
	private static DbSetupTracker dbSetupTracker = new DbSetupTracker();

	@BeforeClass
	public static void setupClass() {
		startApplicationDatabaseForTesting();
		dataSource = DriverManagerDestination.with(DB_URL, DB_USERNAME, DB_PASSWORD);
	}

	@Before
	public void setup() throws SQLException {

		Operation initDBOperations = Operations.sequenceOf(DELETE_ALL, INSERT_CUSTOMER_SALE_DATA);
		DbSetup dbSetup = new DbSetup(dataSource, initDBOperations);

		// Use the tracker to launch the DbSetup. This will speed-up tests
		// that do not not change the BD. Otherwise, just use dbSetup.launch();
		dbSetupTracker.launchIfNecessary(dbSetup);
	}

	/**
	 * Teste que após apagar um cliente existente verifica que as suas sales e as
	 * deliveries são eliminadas das base de dados
	 * 
	 * ponto e)
	 * 
	 * @throws ApplicationException
	 */
	@Test
	public void deleteCustomerAndDeliveries() throws ApplicationException {
		List<CustomerDTO> listCustomers = CustomerService.INSTANCE.getAllCustomers().customers;
		// obter o primeiro customer de todos
		CustomerDTO customer = CustomerService.INSTANCE.getCustomerByVat(197672337); // ele deve ter duas sales
		int numberOfCustomersBefore = listCustomers.size();
		assertNotNull(customer);
		assertNotEquals(0, numberOfCustomersBefore);

		SalesDTO sales = SaleService.INSTANCE.getSaleByCustomerVat(customer.vat);
		// customer tem pelo menos uma sale
		assertFalse(sales.sales.isEmpty());
		int numberOfSalesBefore = sales.sales.size();
		assertNotEquals(0, numberOfSalesBefore); // prova que tem sales
		System.out.print(numberOfSalesBefore);

		SalesDeliveryDTO salesDel = SaleService.INSTANCE.getSalesDeliveryByVat(customer.vat);
		int numberOfSalesDeliveryBefore = salesDel.sales_delivery.size();
		assertNotEquals(0, numberOfSalesDeliveryBefore); // prova que tem sales deliveries
		System.out.print(numberOfSalesDeliveryBefore);

		// remover o customer
		try {
			CustomerService.INSTANCE.removeCustomer(customer.vat);
		} catch (ApplicationException e) {
			assertEquals(e.getMessage(), "Customer with vat number " + customer.vat + " doesn't exist.");
		}

		// remover sales deliveries

		try {
			SaleService.INSTANCE.removeDeliverySalesByCustomerId(customer.vat);
		} catch (ApplicationException e) {
			assertEquals(e.getMessage(),
					"Sale Deliveries of the customer with vat number " + customer.vat + " don't exist.");
		}

		// remover sales
		try {
			SaleService.INSTANCE.removeSaleByCustomerId(customer.vat);
		} catch (ApplicationException e) {
			assertEquals(e.getMessage(), "Sale of the customer with vat number " + customer.vat + " don't exist.");
		}

		// verificar que o customer foi removido
		List<CustomerDTO> listCustomersAfter = CustomerService.INSTANCE.getAllCustomers().customers;
		for (CustomerDTO customerDTO : listCustomersAfter) {
			assertNotEquals(customer.designation, customerDTO.designation);
		}

		int numberOfCustomersAfter = listCustomersAfter.size();
		assertEquals(numberOfCustomersAfter + 1, numberOfCustomersBefore);

		// verificar que o customer não tem sales

		try {
			SalesDTO salesDTO = SaleService.INSTANCE.getSaleByCustomerVat(customer.vat);
			int numberOfSalesAfter = salesDTO.sales.size();
			assertEquals(0, numberOfSalesAfter);
			assertEquals(numberOfSalesAfter + 2, numberOfSalesBefore);
		} catch (ApplicationException e) {
			assertEquals(e.getMessage(), "Invalid VAT number: " + customer.vat);
		}

		// verificar que o customer não tem sales deliverys
		try {

			SalesDeliveryDTO salesDelDTO = SaleService.INSTANCE.getSalesDeliveryByVat(customer.vat);
			int numberOfSalesDeliveryAfter = salesDelDTO.sales_delivery.size();
			assertEquals(0, numberOfSalesDeliveryAfter);// era suposto ser Equals, pois as sales deviam ter sido
														// removidas
			assertEquals(numberOfSalesDeliveryAfter + 1, numberOfSalesDeliveryBefore);// era suposto ser equals

		} catch (ApplicationException e) {
			assertEquals(e.getMessage(), "Customer with vat number " + customer.vat + " not found.");
		}

	}

	/**
	 * Teste que verifica que ao adicionar uma nova delivery verifica que o numero
	 * de deliveries aumentou mais um
	 * 
	 * ponto f)
	 * 
	 * @throws ApplicationException
	 */
	@Test
	public void addSaleDelivery() throws ApplicationException {
		int vat = 197672337;
		// obter o primeiro customer de todos
		CustomerDTO customer = CustomerService.INSTANCE.getCustomerByVat(vat); // ele deve ter pelo menos uma
																				// saledelivery
		int numberOfSalesBefore = SaleService.INSTANCE.getSalesDeliveryByVat(vat).sales_delivery.size();
		int numberSalesDelAfter = 0;
		assertNotNull(customer);
		assertNotEquals(numberOfSalesBefore, numberSalesDelAfter);

		// adicionar nova sale delivery
		try {
			SaleService.INSTANCE.addSaleDelivery(1, 100);
		} catch (ApplicationException e) {
			assertEquals(e.getMessage(), "Can't add address to cutomer.");
		}

		// verificar que existe mais uma sale delivery de antes

		numberSalesDelAfter += SaleService.INSTANCE.getSalesDeliveryByVat(vat).sales_delivery.size();

		assertEquals(numberSalesDelAfter, numberOfSalesBefore + 1);

	}

	/**
	 * Teste extra que verifica que ao adicionar uma nova sale verifica que o numero
	 * de sales do cliente e sales totais aumentou mais um
	 * 
	 * 
	 * 
	 * @throws ApplicationException
	 */
	@Test
	public void addSale() throws ApplicationException {
		int vat = 197672337;
		// obter o primeiro customer de todos
		CustomerDTO customer = CustomerService.INSTANCE.getCustomerByVat(vat); // ele deve ter pelo menos uma
																				// saledelivery
		int numberOfSalesCustomerBefore = SaleService.INSTANCE.getSaleByCustomerVat(vat).sales.size();
		int numberSalesCustomerAfter = 0;
		int numberSalesOverallBefore = SaleService.INSTANCE.getAllSales().sales.size();
		assertNotEquals(0, numberSalesOverallBefore);

		assertNotNull(customer);
		assertNotEquals(numberOfSalesCustomerBefore, numberSalesCustomerAfter);

		// adicionar nova sale delivery
		try {
			SaleService.INSTANCE.addSale(vat);
		} catch (ApplicationException e) {
			assertEquals(e.getMessage(), "Can't add customer with vat number " + vat + ".");
		}

		// verificar que existe mais uma sale delivery de antes

		numberSalesCustomerAfter += SaleService.INSTANCE.getSaleByCustomerVat(vat).sales.size();
		int numberSalesOverallAfter = SaleService.INSTANCE.getAllSales().sales.size();

		assertEquals(numberSalesCustomerAfter, numberOfSalesCustomerBefore + 1);
		assertEquals(numberSalesOverallAfter, numberSalesOverallBefore + 1);

	}

	/**
	 * Teste extra que tenta adicionar uma sales delivery com um número de sale
	 * inválido
	 * 
	 * @throws ApplicationException
	 */
	@Test
	public void addSaleDeliveryInvalidSaleId() {
		int invalidSaleId = 1971;

		// Verifica-se que a ApplicationException é lançada
		assertThrows(ApplicationException.class, () -> {
			SaleService.INSTANCE.addSaleDelivery(invalidSaleId, 200);
		});

	}

	/**
	 * Teste extra que tenta adicionar duas sales delivery com o mesmo número de
	 * salee com o id_addr
	 * 
	 * 
	 * @throws ApplicationException
	 */
	@Test
	public void addTwoSaleDeliveryWithSameSaleId() {
		int SaleId = 1;

		try {
			SaleService.INSTANCE.addSaleDelivery(SaleId, 200);
			SaleService.INSTANCE.addSaleDelivery(SaleId, 200);
		} catch (ApplicationException e) {
			assertEquals(e.getMessage(), "Can't add address to cutomer.");
		}

	}

	/**
	 * Teste extra que verifica se é possível remover todas as sales de um cliente
	 * dado o seu vat
	 * 
	 * 
	 * 
	 * @throws ApplicationException
	 */
	@Test
	public void removeSales() throws ApplicationException {
		// Adiciona uma nova sale para teste
		int customerVat = 197672337;
		SaleService.INSTANCE.addSale(customerVat);
		// Obtém a última sale adicionada
		List<SaleDTO> salesList = SaleService.INSTANCE.getSaleByCustomerVat(customerVat).sales;
		int numberOfSalesBefore = salesList.size();
		assertNotNull(salesList);
		assertNotEquals(0, salesList.size());

		// Remove as sales do Cliente

		try {
			SaleService.INSTANCE.removeSaleByCustomerId(customerVat);
			;
		} catch (ApplicationException e) {
			assertEquals(e.getMessage(), "Sale of the customer with vat number " + customerVat + " don't exist.");
		}

		// verificar se todas as sales forma removidas
		try {
			SalesDTO salesDTO = SaleService.INSTANCE.getSaleByCustomerVat(customerVat);
			int numberOfSalesAfter = salesDTO.sales.size();
			assertEquals(0, numberOfSalesAfter);
			assertEquals(numberOfSalesAfter + 3, numberOfSalesBefore);
		} catch (ApplicationException e) {
			assertEquals(e.getMessage(), "Invalid VAT number: " + customerVat);
		}

	}

	/**
	 * Teste extra que tenta adicionar uma sale com um cliente inexistente.
	 * 
	 * @throws ApplicationException
	 */
	@Test
	public void addSaleWithVatInvalid() throws ApplicationException {
		int vat = 000000000;
		try {
			SaleService.INSTANCE.addSale(vat);
		} catch (ApplicationException e) {
			assertEquals(e.getMessage(), "Invalid VAT number: " + vat);
		}
	}

}
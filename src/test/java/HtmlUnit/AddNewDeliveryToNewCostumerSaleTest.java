package HtmlUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class AddNewDeliveryToNewCostumerSaleTest {

    private static WebClient webClient;
    private static final String APPLICATION_URL = "http://localhost:8080/VVS_webappdemo/";

    private static final String CUSTOMER_VAT = "123456789";
    private static final String DESIGNATION = "Jeff";
    private static final String PHONE = "912345678";
    private static String SALE_ID = ""; // Variável para guardar o ID da venda
    private static final String NEW_SALE_DELIVERY_URL = APPLICATION_URL + "saleDeliveryVat.html";
    private static final String ADD_SALE_DELIVERY_URL = APPLICATION_URL + "AddSaleDeliveryPageController";
    private static final String GET_SALE_PAGE_URL = APPLICATION_URL + "GetSalePageController?customerVat=" + CUSTOMER_VAT;
    private static final String NEW_SALE_URL = APPLICATION_URL + "addSale.html";
    private static final String GET_SALES_URL = APPLICATION_URL + "getSales.html";


    private int nRowsBefore;
    private int initialNumberOfSales;

    @BeforeClass
    public static void setUpClass() throws Exception {
        webClient = new WebClient(BrowserVersion.getDefault());

        // Configurações necessárias para evitar falhas em páginas HTML complexas
        webClient.setJavaScriptTimeout(15000);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
    }

    @AfterClass
    public static void takeDownClass() {
        webClient.close();
    }

    
    @Before
    public void goToNewCustomerPage() throws IOException {
    	
    	try (final WebClient webClient = new WebClient(BrowserVersion.getDefault())) {
    		
    		// Obtém o número de clientes antes de executar o teste
            HtmlPage allCustomersPage = webClient.getPage(APPLICATION_URL + "GetAllCustomersPageController");
            nRowsBefore = allCustomersPage.getElementsByTagName("tr").size();

            // Verifica se os dois clientes anteriores existem na página
            assertFalse(allCustomersPage.asText().contains(CUSTOMER_VAT));
            
            
            // Obtém o número inicial de vendas do cliente
            HtmlPage salesPage = webClient.getPage(GET_SALE_PAGE_URL);
            
            initialNumberOfSales = salesPage.getElementsByTagName("tr").size()-1;
        }
    	
    }

    @Test
    public void addSaleAndDeliveryTest() throws IOException {
    	
    	try (final WebClient webClient = new WebClient(BrowserVersion.getDefault())){
    		
    		
    		//ADICIONAR CLIENTE
        	
        	// Vai para a página de adicionar novo cliente
            HtmlPage newCustomerPage = webClient.getPage(APPLICATION_URL + "addCustomer.html");
            assertEquals("Enter Name", newCustomerPage.getTitleText()); // Verifica o título da página

            // Preenche o formulário para adicionar novo cliente
            List<HtmlForm> forms = newCustomerPage.getForms();
            HtmlForm customerForm = forms.get(0);
            
            fillCustomerForm(customerForm, CUSTOMER_VAT, DESIGNATION, PHONE);
            customerForm.getInputByValue("Get Customer").click(); // Submete o formulário

            // Verifica se os novos clientes foram inseridos corretamente
            newCustomerPage = webClient.getPage(APPLICATION_URL + "GetAllCustomersPageController");
            String reportContent = newCustomerPage.asXml();

            assertTrue(reportContent.contains(CUSTOMER_VAT));
            assertTrue(reportContent.contains(DESIGNATION));
            assertTrue(reportContent.contains(PHONE));
            
            // Verificar se o número de linhas aumentou em 1
            int nRowsAfter = newCustomerPage.getElementsByTagName("tr").size();
            assertEquals(nRowsBefore + 1, nRowsAfter);
            
            
            //------------------------------------------------------//
            
            //ADICIONAR SALE
            
            // Acessa a página para adicionar uma nova venda
            HtmlPage newSalePage = webClient.getPage(NEW_SALE_URL);
            assertEquals("New Sale", newSalePage.getTitleText()); // Verifica se o título da página é "New Sale"

            // Preenche o formulário com o VAT do cliente
            HtmlForm saleForm = newSalePage.getForms().get(0);
            saleForm.getInputByName("customerVat").setValueAttribute(CUSTOMER_VAT);
            newSalePage.getElementById("botao").click(); // Submete o formulário

            // Verifica se o número de vendas do cliente aumentou por 1
            HtmlPage salesPage = webClient.getPage(GET_SALES_URL);
            HtmlForm salesForm = salesPage.getForms().get(0);
            salesForm.getInputByName("customerVat").setValueAttribute(CUSTOMER_VAT);
            salesPage.getElementById("botao").click();

            // Verifica se a página de detalhes da venda do cliente eh acessada
            HtmlPage saleDetailsPage = webClient.getPage(GET_SALE_PAGE_URL);

            // Verifica se a última venda tem o estado 'O' (aberto) e o VAT do cliente correto
            String lastSaleStatus = "";
            String lastSaleVat = "";
            DomElement lastSaleRow = saleDetailsPage.getElementsByTagName("tr").get(saleDetailsPage.getElementsByTagName("tr").size() - 1); //para obter o ultimo elemento aka ultima sale adicionada
            
            //obter informcoes da ultima sale
            lastSaleStatus = lastSaleRow.getElementsByTagName("td").get(3).getTextContent();
            lastSaleVat = lastSaleRow.getElementsByTagName("td").get(4).getTextContent();

            assertEquals("O", lastSaleStatus); // Verifica se o estado da última venda é 'O' (aberto)
            assertEquals(CUSTOMER_VAT, lastSaleVat); // Verifica se o VAT da última venda é o mesmo do cliente
            int finalNumberOfSales = saleDetailsPage.getElementsByTagName("tr").size()-1;
            assertEquals(initialNumberOfSales+1, finalNumberOfSales); // Verifica se o número de vendas aumentou por 1
     

            // Obtém o ID da última venda adicionada
            SALE_ID = lastSaleRow.getElementsByTagName("td").get(0).getTextContent();
            
            
            //------------------------------------------------------//
            
            //ADICIONAE NOVA DELIVERY

            // Vai para a página de adicionar entrega para a venda
            HtmlPage newSaleDeliveryPage = webClient.getPage(NEW_SALE_DELIVERY_URL);
            assertEquals("Enter Name", newSaleDeliveryPage.getTitleText()); // Verifica o título da página

            // Preenche o formulário para adicionar entrega para a venda
            HtmlForm saleDeliveryForm = newSaleDeliveryPage.getForms().get(0);
            saleDeliveryForm.getInputByName("vat").setValueAttribute(CUSTOMER_VAT);
            HtmlPage nextPage = saleDeliveryForm.getInputByValue("Get Customer").click(); // Submete o formulário

            // Verifica se foi redirecionado corretamente
            assertTrue(nextPage.getUrl().toString().contains("AddSaleDeliveryPageController?vat=" + CUSTOMER_VAT));

            Random random = new Random();
            
            // número aleatório entre 3 e 10000
            int addr_id = random.nextInt(9998) + 3;
            
            
            // Preenche o formulário para adicionar entrega para a venda
            saleDeliveryForm = nextPage.getForms().get(0);
            saleDeliveryForm.getInputByName("addr_id").setValueAttribute(String.valueOf(addr_id)); // Endereço de entrega fictício
            saleDeliveryForm.getInputByName("sale_id").setValueAttribute(SALE_ID);
            nextPage = saleDeliveryForm.getInputByValue("Insert").click(); // Submete o formulário

            // Verifica se foi redirecionado corretamente
            assertTrue(nextPage.getUrl().toString().contains("AddSaleDeliveryPageController"));

            // Verifica se a entrega foi adicionada corretamente
            assertTrue(nextPage.getTitleText().contains("Sales Info"));
            assertTrue(nextPage.asXml().contains(SALE_ID)); // Verifica se o ID da venda está na página
            assertTrue(nextPage.asXml().contains(String.valueOf(addr_id))); // Verifica se o ID do endereço de entrega está na página
    	}
    	
    	
    }
    
    private void fillCustomerForm(HtmlForm form, String vat, String designation, String phone) {
        // Preenche o formulário com os dados do cliente
        form.getInputByName("vat").setValueAttribute(vat);
        form.getInputByName("designation").setValueAttribute(designation);
        form.getInputByName("phone").setValueAttribute(phone);
    }
}

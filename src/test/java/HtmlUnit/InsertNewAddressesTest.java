package HtmlUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;


public class InsertNewAddressesTest {

    private static HtmlPage page;
    private static WebClient webClient;
    private static final String APPLICATION_URL =  "http://localhost:8080/VVS_webappdemo/";

    private static final String VAT = "168027852";
    private static final String ADDRESS_1 = "RUA NUMERO 1";
    private static final String DOOR_1 = "10";
    private static final String POSTALCODE_1 = "2800-081";
    private static final String LOCALITY_1 = "Almada";

    private static final String ADDRESS_2 = "RUA NUMERO 2";
    private static final String DOOR_2 = "15";
    private static final String POSTALCODE_2 = "2800-081";
    private static final String LOCALITY_2 = "Almada";

    private int nRowsBefore;

    @BeforeClass
	public static void setUpClass() throws Exception {
		webClient = new WebClient(BrowserVersion.getDefault());

		// possible configurations needed to prevent JUnit tests to fail for complex
		// HTML pages
		webClient.setJavaScriptTimeout(15000);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setCssEnabled(false);
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());

		page = webClient.getPage(APPLICATION_URL);
		assertEquals(200, page.getWebResponse().getStatusCode()); // OK status
	}

	@AfterClass
	public static void takeDownClass() {
		webClient.close();
	}

    @Before
    public void getNumberRows() throws IOException {
        // Obter o número de linhas da tabela antes de executar o teste
        try (final WebClient webClient = new WebClient(BrowserVersion.getDefault())) {
            // Carregar a página que mostra as informacoes do cliente com o numero de contribuinte especificado
            HtmlPage reportPage = webClient.getPage(APPLICATION_URL + "GetCustomerPageController?vat=" + VAT + "&submit=Get+Customer");
            // Obter o número de linhas da tabela
            nRowsBefore = reportPage.getElementsByTagName("tr").size();
        }
    }
    

    @Test
    public void insertNewAddressesTest() throws IOException {
        // Teste para inserir novos endereços para um cliente existente
        try (final WebClient webClient = new WebClient(BrowserVersion.getDefault())) {
            // Obter o link para a página de adicionar cliente
            DomElement addCustomerButton = page.getElementsById("botao2").get(1);
            // Clicar no link para abrir o formulário de adicionar endereço
            HtmlPage nextPage = addCustomerButton.click();
            // Verificar se o título da página é "Add Address"
            assertEquals("Enter Address", nextPage.getTitleText());
            

            // Obter o formulário de adicionar endereço
            List<HtmlForm> forms = nextPage.getForms();
            assertTrue("Formulário de adicionar endereço não encontrado", forms.size() > 0);
            HtmlForm insertAddressForm = forms.get(0);

            // Inserir os endereços
            fillAddressForm(insertAddressForm, VAT, ADDRESS_1, DOOR_1, POSTALCODE_1, LOCALITY_1);
            nextPage.getHtmlElementById("botao").click(); //submit
            
            
            nextPage = webClient.getPage(APPLICATION_URL + "addAddressToCustomer.html");

            
            // Obter o formulário de adicionar endereço novamente após a página ser recarregada
            forms = nextPage.getForms();
            assertTrue("Formulário de adicionar endereço não encontrado", forms.size() > 0);
            insertAddressForm = forms.get(0);

            fillAddressForm(insertAddressForm, VAT, ADDRESS_2, DOOR_2, POSTALCODE_2, LOCALITY_2);
            nextPage.getHtmlElementById("botao").click(); // submit
            
            nextPage = webClient.getPage(APPLICATION_URL + "GetCustomerPageController?vat=" + VAT + "&submit=Get+Customer");


            // Verificar se ambos os endereços foram inseridos corretamente
            String reportContent = nextPage.asXml();

            assertTrue(reportContent.contains(ADDRESS_1));
            assertTrue(reportContent.contains(DOOR_1));
            assertTrue(reportContent.contains(POSTALCODE_1));
            assertTrue(reportContent.contains(LOCALITY_1));

            assertTrue(reportContent.contains(ADDRESS_2));
            assertTrue(reportContent.contains(DOOR_2));
            assertTrue(reportContent.contains(POSTALCODE_2));
            assertTrue(reportContent.contains(LOCALITY_2));

            // Verificar se o número de linhas aumentou em 2
            int nRowsAfter = nextPage.getElementsByTagName("tr").size();
            assertEquals(nRowsBefore + 2, nRowsAfter); // Dois novos endereços adicionados
        }
    }


    private void fillAddressForm(HtmlForm form, String vat, String address, String door, String postalCode, String locality) {
        // Preenche o formulário com os dados do endereço
        form.getInputByName("vat").setValueAttribute(vat);
        form.getInputByName("address").setValueAttribute(address);
        form.getInputByName("door").setValueAttribute(door);
        form.getInputByName("postalCode").setValueAttribute(postalCode);
        form.getInputByName("locality").setValueAttribute(locality);
    }

    
    

}

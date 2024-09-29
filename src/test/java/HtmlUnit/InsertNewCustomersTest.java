package HtmlUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class InsertNewCustomersTest {

    private static HtmlPage page;
    private static WebClient webClient;
    private static final String APPLICATION_URL = "http://localhost:8080/VVS_webappdemo/";

    private static final String VAT_1 = "123456789";
    private static final String DESIGNATION_1 = "Miguel";
    private static final String PHONE_1 = "989898989";

    private static final String VAT_2 = "989898989";
    private static final String DESIGNATION_2 = "Tiago";
    private static final String PHONE_2 = "987654321";

    private int nRowsBefore;

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

        // Carrega a página inicial
        page = webClient.getPage(APPLICATION_URL);
        assertEquals(200, page.getWebResponse().getStatusCode()); // Status OK
    }

    @AfterClass
    public static void takeDownClass() {
        webClient.close();
    }

    @Before
    public void getNumberRows() throws IOException {
        // Obtém o número de clientes antes de executar o teste
        HtmlPage allCustomersPage = webClient.getPage(APPLICATION_URL + "GetAllCustomersPageController");
        nRowsBefore = allCustomersPage.getElementsByTagName("tr").size();

        // Verifica se os dois clientes anteriores existem na página
        assertFalse(allCustomersPage.asText().contains(VAT_1));
        assertFalse(allCustomersPage.asText().contains(VAT_2));
    }

    @Test
    public void insertNewCustomersTest() throws IOException {
        
        try (final WebClient webClient = new WebClient(BrowserVersion.getDefault())){
            DomElement addCustomerButton = page.getElementsById("botao2").get(0);
            // Clicar no link para abrir o formulário de adicionar endereço
            HtmlPage nextPage = addCustomerButton.click();
            // Verificar se o título da página é "Add Address"
            assertEquals("Enter Name", nextPage.getTitleText());
            
            List<HtmlForm> forms = nextPage.getForms();
            HtmlForm insertCustomerForm = forms.get(0);
            
            // Preenche o formulário para o primeiro cliente
            fillCustomerForm(insertCustomerForm, VAT_1, DESIGNATION_1, PHONE_1);
            insertCustomerForm.getInputByValue("Get Customer").click(); // Submete o formulário

            nextPage = webClient.getPage(APPLICATION_URL + "index.html");

            // Preenche o formulário para o segundo cliente
            addCustomerButton = nextPage.getElementsById("botao2").get(0);
            // Clicar no link para abrir o formulário de adicionar endereço
            nextPage = addCustomerButton.click();
            assertEquals("Enter Name", nextPage.getTitleText());
            forms = nextPage.getForms();
            insertCustomerForm = forms.get(0);
            fillCustomerForm(insertCustomerForm, VAT_2, DESIGNATION_2, PHONE_2);
            insertCustomerForm.getInputByValue("Get Customer").click(); // Submete o formulário

            
            // Verifica se os novos clientes foram inseridos corretamente
            nextPage = webClient.getPage(APPLICATION_URL + "GetAllCustomersPageController");
            String reportContent = nextPage.asXml();

            assertTrue(reportContent.contains(VAT_1));
            assertTrue(reportContent.contains(DESIGNATION_1));
            assertTrue(reportContent.contains(PHONE_1));

            assertTrue(reportContent.contains(VAT_2));
            assertTrue(reportContent.contains(DESIGNATION_2));
            assertTrue(reportContent.contains(PHONE_2));
            
            // Verificar se o número de linhas aumentou em 2
            int nRowsAfter = nextPage.getElementsByTagName("tr").size();
            assertEquals(nRowsBefore + 2, nRowsAfter); // Dois novos endereços adicionados
        }
        
    }

    private void fillCustomerForm(HtmlForm form, String vat, String designation, String phone) {
        // Preenche o formulário com os dados do cliente
        form.getInputByName("vat").setValueAttribute(vat);
        form.getInputByName("designation").setValueAttribute(designation);
        form.getInputByName("phone").setValueAttribute(phone);
    }
}

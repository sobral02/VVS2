package HtmlUnit;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

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

public class AddNewSaleTest {

    private static WebClient webClient;
    private static final String APPLICATION_URL = "http://localhost:8080/VVS_webappdemo/";

    private static final String CUSTOMER_VAT = "123456789";
    private static final String NEW_SALE_URL = APPLICATION_URL + "addSale.html";
    private static final String GET_SALES_URL = APPLICATION_URL + "getSales.html";
    private static final String GET_SALE_PAGE_URL = APPLICATION_URL + "GetSalePageController?customerVat=" + CUSTOMER_VAT;

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
    public void goToNewSalePage() throws IOException {
        try (final WebClient webClient = new WebClient(BrowserVersion.getDefault())) {
            // Obtém o número inicial de vendas do cliente
            HtmlPage salesPage = webClient.getPage(GET_SALE_PAGE_URL);
            
            initialNumberOfSales = salesPage.getElementsByTagName("tr").size()-1;
        }
    }

    @Test
    public void addNewSaleTest() throws IOException {
        try (final WebClient webClient = new WebClient(BrowserVersion.getDefault())) {
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
        }
    }
}

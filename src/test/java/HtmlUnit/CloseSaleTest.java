package HtmlUnit;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

//Neste teste é verificado se existe alguma sale com estado 'O' e é fechada
public class CloseSaleTest {

    private static WebClient webClient;
    private static final String APPLICATION_URL = "http://localhost:8080/VVS_webappdemo/";
    private static final String UPDATE_SALE_STATUS_URL = APPLICATION_URL + "UpdateSaleStatusPageControler";

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

    @Test
    public void closeOpenSaleTest() throws IOException {
        HtmlPage updateStatusPage = webClient.getPage(UPDATE_SALE_STATUS_URL);
        assertEquals("Enter Sale Id", updateStatusPage.getTitleText()); // Verifica se o título da página é "Enter Sale Id"

        // Encontra a primeira linha de venda aberta
        DomElement saleRow = null;
        for (DomElement element : updateStatusPage.getElementsByTagName("tr")) {
            if (element.getElementsByTagName("td").size() >= 4) { // Verifica se a linha tem pelo menos 4 colunas
                String saleStatus = element.getElementsByTagName("td").get(3).getTextContent();
                if (saleStatus.equals("O")) { // Se a venda estiver aberta, salva a linha
                    saleRow = element;
                    break;
                }
            }
        }

        // Verifica se encontrou uma venda aberta
        if (saleRow != null) {
            String saleId = saleRow.getElementsByTagName("td").get(0).getTextContent();

            // Preenche o formulário com o ID da venda aberta
            HtmlForm updateStatusForm = updateStatusPage.getForms().get(0);
            updateStatusForm.getInputByName("id").setValueAttribute(saleId);


            // Submete o formulário para fechar a venda
            updateStatusPage = updateStatusPage.getElementById("botao1").click();

            // Verifica se a venda foi fechada com sucesso
            String updatedSaleStatus = "";
            for (DomElement element : updateStatusPage.getElementsByTagName("tr")) {
                if (element.getElementsByTagName("td").size() >= 4) { // Verifica se a linha tem pelo menos 4 colunas
                    String currentSaleId = element.getElementsByTagName("td").get(0).getTextContent();
                    if (currentSaleId.equals(saleId)) { // Encontra a linha da venda atualizada
                        updatedSaleStatus = element.getElementsByTagName("td").get(3).getTextContent();
                        break;
                    }
                }
            }

            assertEquals("C", updatedSaleStatus); // Verifica se a venda foi fechada com sucesso
        }
    }
}

package MokitoTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.Test;

import webapp.services.*;

public class CustomerServiceTest {

    @Test
    public void testGetCustomerByVat() throws ApplicationException {
        // Criar um mock para simular o CustomerServiceMockito
        CustomerServiceMockito mockedCustomerServiceMockito = mock(CustomerServiceMockito.class);

        // Definir o comportamento do metodo getCustomerByVat do CustomerServiceMockito
        when(mockedCustomerServiceMockito.getCustomerByVat(anyInt())).thenReturn(new CustomerDTO(1, 123456789, "Goncalo", 123456789));

        // Testar o metodo getCustomerByVat
        CustomerDTO customerDTO = mockedCustomerServiceMockito.getCustomerByVat(123456789);

        // Verificar se o método do CustomerServiceMockito foi chamado
        verify(mockedCustomerServiceMockito).getCustomerByVat(123456789);

        // Asserts para verificar os dados retornados 
        assertEquals(1, customerDTO.id);
        assertEquals(123456789, customerDTO.vat);
        assertEquals("Goncalo", customerDTO.designation);
        assertEquals(123456789, customerDTO.phoneNumber);
    }
}
	
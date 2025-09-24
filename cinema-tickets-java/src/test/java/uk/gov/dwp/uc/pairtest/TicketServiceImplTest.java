package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class TicketServiceImplTest {

    @Mock
    private TicketPaymentService ticketPaymentService;

    @Mock
    private SeatReservationService seatReservationService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Test
    void purchaseTickets_SuccessfulPurchase_CallsServicesWithCorrectValues() {
        Long accountId = 1L;
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        ticketService.purchaseTickets(accountId, adultRequest, childRequest, infantRequest);

        int expectedPayment = (2 * 25) + (1 * 15);
        int expectedSeats = 2 + 1;

        verify(ticketPaymentService).makePayment(accountId, expectedPayment);
        verify(seatReservationService).reserveSeat(accountId, expectedSeats);
    }

    @Test
    void purchaseTickets_ThrowsException_WhenAccountIdIsInvalid() {
        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(0L);
        });
    }
    
    @Test
    void purchaseTickets_ThrowsException_WhenNoTicketsRequested() {
        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(1L);
        });
    }

    @Test
    void purchaseTickets_ThrowsException_WhenMoreThanMaxTicketsPurchased() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26);
        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(1L, adultRequest);
        });
    }
    
    @Test
    void purchaseTickets_DoesNotThrowException_WhenExactlyMaxTicketsPurchased() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 25);
        ticketService.purchaseTickets(1L, adultRequest);
        verify(ticketPaymentService).makePayment(1L, 25 * 25);
        verify(seatReservationService).reserveSeat(1L, 25);
    }

    @Test
    void purchaseTickets_ThrowsException_WhenChildTicketPurchasedWithoutAdult() {
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(1L, childRequest);
        });
    }

    @Test
    void purchaseTickets_ThrowsException_WhenInfantTicketPurchasedWithoutAdult() {
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(1L, infantRequest);
        });
    }
    
    @Test
    void purchaseTickets_ThrowsException_WhenInfantsExceedAdults() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        
        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(1L, adultRequest, infantRequest);
        });
    }

    @Test
    void purchaseTickets_InfantsAndAdultsOnly_CalculatesCorrectly() {
        Long accountId = 123L;
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        ticketService.purchaseTickets(accountId, adultRequest, infantRequest);

        verify(ticketPaymentService).makePayment(accountId, 50);
        verify(seatReservationService).reserveSeat(accountId, 2);
    }
    
    @Test
    void purchaseTickets_DoesNotCallServices_WhenValidationFails() {
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        
        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(1L, childRequest);
        });

        verify(ticketPaymentService, never()).makePayment(1L, 15);
        verify(seatReservationService, never()).reserveSeat(1L, 1);
    }
}
document.addEventListener('DOMContentLoaded', () => {
    const approveButton = document.getElementById('approveButton');
    const denyButton = document.getElementById('denyButton');
    const reservationIdSpan = document.getElementById('reservationId');
    const amountSpan = document.getElementById('amount');
    const statusMessageDiv = document.getElementById('statusMessage');

    const params = new URLSearchParams(window.location.search);
    const reservationId = params.get('reservationId');
    const amount = params.get('amount');

    if (reservationId && amount) {
        reservationIdSpan.textContent = reservationId;
        amountSpan.textContent = parseFloat(amount).toFixed(2);
    } else {
        statusMessageDiv.textContent = 'Error: Reservation details not found in URL.';
        approveButton.disabled = true;
        denyButton.disabled = true;
    }

    const handlePaymentAction = async (status) => {
        if (!reservationId) return;

        try {
            const response = await fetch('/api/payment/notify', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    reservationId: reservationId,
                    status: status, 
                }),
            });

            if (response.ok) {
                statusMessageDiv.textContent = `Payment has been ${status}. You can close this window.`;
                approveButton.disabled = true;
                denyButton.disabled = true;
            } else {
                statusMessageDiv.textContent = 'An error occurred. Please try again.';
            }
        } catch (error) {
            console.error('Error sending payment notification:', error);
            statusMessageDiv.textContent = 'Could not connect to the server.';
        }
    };

    approveButton.addEventListener('click', () => handlePaymentAction('approved'));
    denyButton.addEventListener('click', () => handlePaymentAction('denied'));
});
document.addEventListener('DOMContentLoaded', () => {
    const destinationSelect = document.getElementById('destination');
    const departureDateInput = document.getElementById('departureDate');
    const embarkationPortInput = document.getElementById('embarkationPort');
    const searchItinerariesButton = document.getElementById('searchItinerariesButton');
    const itinerariesResultDiv = document.getElementById('itinerariesResult');
    const cancelReservationButton = document.getElementById('cancelReservationButton');
    const clientNameInput = document.getElementById('clientName');
    const selectedItineraryIdInput = document.getElementById('selectedItineraryId');
    const numPassengersInput = document.getElementById('numPassengers');
    const numCabinsInput = document.getElementById('numCabins');
    const makeReservationButton = document.getElementById('makeReservationButton');
    const reservationStatusDiv = document.getElementById('reservationStatus');
    const reservationCodeCancelInput = document.getElementById('reservationCodeCancel');
    const notificationsDiv = document.getElementById('notifications');
    const promotionsDiv = document.getElementById('promotions');
    const registerPromotionsButton = document.getElementById('registerPromotionsButton');
    const cancelPromotionsButton = document.getElementById('cancelPromotionsButton');
    const connectSseButton = document.getElementById('connectSseButton');

    let selectedItineraryDetails = null;
    let currentSearchYear = null;
    let currentSearchMonth = null;

    let eventSource = null;
    let isSubscribedToNotifications = false;
    let isSubscribedToPromotions = false;

    const embarkationPortsByDestination = {
        'Bahamas': ['Miami', 'Orlando'],
        'Italy': ['Rome', 'Naples'],
        'Brazil': ['Rio de Janeiro', 'Fortaleza', 'Santos', 'Manaus'],
        'Norway': ['Oslo']
    };

    destinationSelect.addEventListener('change', () => {
        const selectedDestination = destinationSelect.value;
        embarkationPortInput.innerHTML = '';
        if (selectedDestination) {
            embarkationPortInput.disabled = false;
            const ports = embarkationPortsByDestination[selectedDestination] || [];
            let defaultOption = new Option("Select an Embarkation Port", "");
            embarkationPortInput.add(defaultOption);
            ports.forEach(port => {
                let option = new Option(port, port);
                embarkationPortInput.add(option);
            });
        } else {
            let defaultOption = new Option("Select a Destination First", "");
            embarkationPortInput.add(defaultOption);
            embarkationPortInput.disabled = true;
        }
    });

    if (searchItinerariesButton) {
        searchItinerariesButton.addEventListener('click', searchItineraries);
    }
    if (makeReservationButton) {
        makeReservationButton.addEventListener('click', handleMakeReservation);
    }
    if (cancelReservationButton) {
        cancelReservationButton.addEventListener('click', handleCancelReservation);
    }
    if (connectSseButton) {
        connectSseButton.addEventListener('click', signIn);
    }
    if (registerPromotionsButton) {
        registerPromotionsButton.addEventListener('click', subscribeToPromotions);
    }
    if (cancelPromotionsButton) {
        cancelPromotionsButton.addEventListener('click', unsubscribeFromPromotions);
    }

    async function searchItineraries() {
        itinerariesResultDiv.innerHTML = '';
        selectedItineraryIdInput.value = '';
        selectedItineraryDetails = null;
        reservationStatusDiv.innerHTML = '';

        const destination = destinationSelect.value;
        const departureDateValue = departureDateInput.value;
        const embarkationPort = embarkationPortInput.value.trim();

        if (!destination) {
            displayError(itinerariesResultDiv,'Please select a destination.');
            return;
        }
        if (!departureDateValue) {
            displayError(itinerariesResultDiv,'Please select a departure year and month.');
            return;
        }
        if (!embarkationPort) {
            displayError(itinerariesResultDiv,'Please enter an embarkation port.');
            return;
        }

        const [yearStr, monthStr] = departureDateValue.split('-');
        currentSearchYear = parseInt(yearStr, 10);
        currentSearchMonth = parseInt(monthStr, 10);

        const headers = new Headers();
        headers.append('destination', destination);
        headers.append('year', currentSearchYear.toString());
        headers.append('month', currentSearchMonth.toString());
        headers.append('embarkationPort', embarkationPort);

        try {
            const response = await fetch('/available-itineraries', {
                method: 'GET',
                headers: headers
            });

            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(`HTTP error ${response.status}: ${errorData || 'Failed to fetch itineraries'}`);
            }

            const itineraries = await response.json();
            displayItineraries(itineraries, currentSearchYear, currentSearchMonth);

        } catch (error) {
            console.error('Error fetching itineraries:', error);
            displayError(itinerariesResultDiv,`Error fetching itineraries: ${error.message}`);
        }
    }

    function displayItineraries(itineraries, selectedYear, selectedMonth) {
        if (!itineraries || itineraries.length === 0) {
            itinerariesResultDiv.innerHTML = '<p>No itineraries found for your selection.</p>';
            return;
        }

        const ul = document.createElement('ul');
        ul.classList.add('itineraries-list');

        itineraries.forEach((itinerary, index) => {
            const li = document.createElement('li');
            li.classList.add('itinerary-item');
            const displayId = `${itinerary.shipName.replace(/\s+/g, '')}-${itinerary.departureDayOfMonth}-${index}`;

            let departureDateStr = "N/A";
            try {
                const departureDate = new Date(selectedYear, selectedMonth - 1, itinerary.departureDayOfMonth);
                if (departureDate.getFullYear() === selectedYear && departureDate.getMonth() === (selectedMonth - 1) && departureDate.getDate() === itinerary.departureDayOfMonth) {
                    departureDateStr = departureDate.toLocaleDateString();
                } else {
                     departureDateStr = `Day ${itinerary.departureDayOfMonth} (check month/year validity for this day)`;
                }
            } catch (e) {
                console.warn("Could not form valid date for itinerary:", itinerary, e);
            }

            const itineraryJson = JSON.stringify(itinerary);

            li.innerHTML = `
                <h3>${itinerary.shipName} (Ref: ${displayId.substring(0, 15)}...)</h3>
                <p><strong>Departure Date:</strong> ${departureDateStr} (Departs on day ${itinerary.departureDayOfMonth} of the month)</p>
                <p><strong>Route:</strong> ${itinerary.embarkationPort} to ${itinerary.disembarkationPort}</p>
                <p><strong>Visits:</strong> ${itinerary.visitedPlaces ? itinerary.visitedPlaces.join(', ') : 'N/A'}</p>
                <p><strong>Nights:</strong> ${itinerary.nights}</p>
                <p><strong>Price per Person:</strong> $${itinerary.pricePerPerson ? itinerary.pricePerPerson.toFixed(2) : 'N/A'}</p>
                <p><strong>Cabins:</strong> ${itinerary.availableCabins} available / ${itinerary.maxCabins} total</p>
                <p><strong>Passengers:</strong> ${itinerary.availablePassengers} available / ${itinerary.maxPassengers} total</p>
                <button class="select-itinerary-button" data-itinerary-id="${displayId}" data-itinerary-details='${itineraryJson}'>Select for Reservation</button>
            `;
            ul.appendChild(li);
        });

        itinerariesResultDiv.appendChild(ul);

        document.querySelectorAll('.select-itinerary-button').forEach(button => {
            button.addEventListener('click', (event) => {
                const clickedButton = event.target;
                const itineraryId = clickedButton.getAttribute('data-itinerary-id');
                const itineraryDataString = clickedButton.getAttribute('data-itinerary-details');

                selectedItineraryIdInput.value = itineraryId;

                try {
                    selectedItineraryDetails = JSON.parse(itineraryDataString);
                    if (selectedItineraryDetails && selectedItineraryDetails.shipName) {
                        reservationStatusDiv.innerHTML = `<p class="success">Selected Itinerary: ${selectedItineraryDetails.shipName} (Ref: ${itineraryId.substring(0, 15)}...). Please fill passenger/cabin details and click 'Make Reservation'.</p>`;
                    } else {
                        displayError(reservationStatusDiv, "Error selecting itinerary: details incomplete after parsing.");
                        selectedItineraryDetails = null;
                    }
                } catch (e) {
                    displayError(reservationStatusDiv, "Error selecting itinerary: could not parse details. Check console for problematic JSON.");
                    selectedItineraryDetails = null;
                }
            });
        });
    }

    async function handleMakeReservation() {
        reservationStatusDiv.innerHTML = '';

        const clientName = clientNameInput.value.trim();
        const numPassengers = parseInt(numPassengersInput.value, 10);
        const numCabins = parseInt(numCabinsInput.value, 10);

        if (!clientName) {
            displayError(reservationStatusDiv, 'Please enter your name.');
            return;
        }
        if (!selectedItineraryDetails || typeof selectedItineraryDetails !== 'object' || Object.keys(selectedItineraryDetails).length === 0) {
            displayError(reservationStatusDiv, 'Please select an itinerary first. (Details not found)');
            return;
        }
        if (isNaN(numPassengers) || numPassengers <= 0) {
            displayError(reservationStatusDiv, 'Please enter a valid number of passengers (must be > 0).');
            return;
        }
        if (isNaN(numCabins) || numCabins <= 0) {
            displayError(reservationStatusDiv, 'Please enter a valid number of cabins (must be > 0).');
            return;
        }
        if (numCabins > selectedItineraryDetails.availableCabins) {
            displayError(reservationStatusDiv, `Not enough cabins available. Requested: ${numCabins}, Available: ${selectedItineraryDetails.availableCabins}`);
            return;
        }
        if (numPassengers > selectedItineraryDetails.availablePassengers) {
             displayError(reservationStatusDiv, `Not enough passenger capacity. Requested: ${numPassengers}, Available: ${selectedItineraryDetails.availablePassengers}`);
            return;
        }
        if (!currentSearchYear || !currentSearchMonth) {
            displayError(reservationStatusDiv, 'Search year and month are missing. Please search for an itinerary again.');
            return;
        }

        const reservationData = {
            clientName: clientName,
            destination: destinationSelect.value,
            shipName: selectedItineraryDetails.shipName,
            embarkationPort: selectedItineraryDetails.embarkationPort,
            disembarkationPort: selectedItineraryDetails.disembarkationPort,
            year: currentSearchYear,
            month: currentSearchMonth,
            departureDayOfMonth: selectedItineraryDetails.departureDayOfMonth,
            numPassengers: numPassengers,
            numCabins: numCabins,
            visitedPlaces: selectedItineraryDetails.visitedPlaces,
            nights: selectedItineraryDetails.nights,
            pricePerPerson: selectedItineraryDetails.pricePerPerson
        };

        try {
            const response = await fetch('/make-reservation', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(reservationData)
            });

            const responseData = await response.json();

            if (!response.ok) {
                const errorMessage = responseData.error || responseData.message || `HTTP error ${response.status}`;
                throw new Error(errorMessage);
            }

            let successMessage = `
                ${responseData.message}<br>
                Your Reservation ID is: <strong>${responseData.reservationId}</strong>
            `;
            if (responseData.paymentLink) {
                successMessage += `
                    <br>Payment Link: <a href="${responseData.paymentLink}" target="_blank" rel="noopener noreferrer">${responseData.paymentLink}</a>
                `;
            }
            displaySuccess(reservationStatusDiv, successMessage);

        } catch (error) {
            const displayErrorMessage = error.message || error.toString();
            displayError(reservationStatusDiv, `Reservation failed: ${displayErrorMessage}`);
        }
    }

    async function handleCancelReservation() {
        const reservationId = reservationCodeCancelInput.value.trim();
        reservationStatusDiv.innerHTML = '';

        if (!reservationId) {
            displayError(reservationStatusDiv, 'Please enter the reservation code to cancel.');
            return;
        }

        try {
            const response = await fetch(`/reservations/${reservationId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            const responseData = await response.json();

            if (!response.ok) {
                const errorMessage = responseData.error || responseData.message || `HTTP error ${response.status}`;
                throw new Error(errorMessage);
            }

            displaySuccess(reservationStatusDiv, `${responseData.message}`);
            reservationCodeCancelInput.value = '';

        } catch (error) {
            const displayErrorMessage = error.message || error.toString();
            displayError(reservationStatusDiv, `Cancellation failed: ${displayErrorMessage}`);
        }
    }

    function signIn() {
        const clientName = clientNameInput.value.trim();
        if (!clientName) {
            addNotification("Please enter your name to sign in.", "error");
            return;
        }
        if (isSubscribedToNotifications) {
            addNotification("You are already signed in.", "info");
            return;
        }
        isSubscribedToNotifications = true;
        addNotification(`Signed in as ${clientName}. General notifications are now active.`, "success");
        initializeSse();
    }

    function subscribeToPromotions() {
        const clientName = clientNameInput.value.trim();
        if (!clientName) {
            addNotification("Please sign in with your name before registering for promotions.", "error");
            return;
        }
        if (isSubscribedToPromotions) {
            addNotification("You are already registered for promotions.", "info");
            return;
        }
        isSubscribedToPromotions = true;
        addNotification("You are now registered for promotional messages.", "success");
        initializeSse();
    }

    function unsubscribeFromPromotions() {
        if (!isSubscribedToPromotions) {
             addNotification("You are not currently registered for promotions.", "info");
             return;
        }
        isSubscribedToPromotions = false;
        addNotification("You will no longer see new promotional messages.", "info");
    }

    function formatReservationDetails(rawData) {
        const parts = rawData.split('Details: ');
        const csvData = parts[1];

        if (!csvData) {
            return rawData;
        }

        const fields = csvData.split(',');
        const reservationId = fields[0];
        const clientName = fields[1];
        const shipName = fields[3];
        const departureDate = fields[6];

        return `
            <strong>for ${clientName}</strong><br>
            <strong>Ship:</strong> ${shipName}<br>
            <strong>Date:</strong> ${departureDate}<br>
            <strong>ID:</strong> <small>${reservationId}</small>
        `;
    }

    function initializeSse() {
        if (eventSource && eventSource.readyState !== EventSource.CLOSED) {
            return;
        }
        const clientName = clientNameInput.value.trim();
        if (!clientName) {
            return;
        }

        addNotification('Connecting to notification service...', 'info');
        eventSource = new EventSource(`/subscribe-notifications/${clientName}`);

        eventSource.onopen = function() {
            addNotification(`Connection to service established for ${clientName}.`, "success");
        };

        eventSource.addEventListener('promotion', function(event) {
            if (isSubscribedToPromotions) {
                addPromotionNotification(event.data);
            }
        });

        eventSource.addEventListener('payment_approved', function(event) {
            if (isSubscribedToNotifications) {
                addNotification("Payment Approved" + formatReservationDetails(event.data), 'success');
            }
        });

        eventSource.addEventListener('payment_denied', function(event) {
            if (isSubscribedToNotifications) {
                addNotification("Payment Denied" + formatReservationDetails(event.data), 'error');
            }
        });

        eventSource.addEventListener('ticket_generated', function(event) {
            if (isSubscribedToNotifications) {
                addNotification("Ticket Generated" +formatReservationDetails(event.data), 'success');
            }
        });

        eventSource.addEventListener('payment_error', function(event) {
            if (isSubscribedToNotifications) {
                addNotification(`Payment Error: ${event.data}`, 'error');
            }
        });

        eventSource.onerror = function(err) {
            if (isSubscribedToNotifications || isSubscribedToPromotions) {
                 addNotification("Notification service error or connection closed.", "error");
            }
            if (eventSource) {
                 eventSource.close();
                 eventSource = null;
            }
        };
    }

    function addNotification(message, type = "info") {
        const p = document.createElement('p');
        p.className = type;
        p.innerHTML = message;

        const firstChild = notificationsDiv.querySelector('p');
        if (firstChild && firstChild.textContent.includes("will appear here...")) {
            notificationsDiv.innerHTML = '';
        }
        notificationsDiv.appendChild(p);
        notificationsDiv.scrollTop = notificationsDiv.scrollHeight;
    }

    function addPromotionNotification(message) {
        const p = document.createElement('p');
        p.textContent = message;
        p.className = 'promotion';

        const firstChild = promotionsDiv.querySelector('p');
        if (firstChild && firstChild.textContent.includes("will appear here...")) {
            promotionsDiv.innerHTML = '';
        }
        promotionsDiv.appendChild(p);
        promotionsDiv.scrollTop = promotionsDiv.scrollHeight;
    }

    function displayError(divElement, message) {
        divElement.innerHTML = `<p class="error">${message}</p>`;
    }

    function displaySuccess(divElement, message) {
        divElement.innerHTML = `<p class="success">${message}</p>`;
    }
});
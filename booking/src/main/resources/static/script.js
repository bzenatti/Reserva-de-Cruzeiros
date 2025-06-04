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

    let selectedItineraryDetails = null;
    let currentSearchYear = null;
    let currentSearchMonth = null;

    if (searchItinerariesButton) {
        searchItinerariesButton.addEventListener('click', searchItineraries);
    }

    if (makeReservationButton) {
        makeReservationButton.addEventListener('click', handleMakeReservation);
    }
    if (cancelReservationButton) {
        cancelReservationButton.addEventListener('click', handleCancelReservation);
    }

    async function searchItineraries() {
        console.log("searchItineraries called. Resetting selectedItineraryDetails.");
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
                
                console.log("Select button clicked. Display ID:", itineraryId);
                console.log("Raw data-itinerary-details string:", itineraryDataString);

                selectedItineraryIdInput.value = itineraryId; 
                
                try {
                    selectedItineraryDetails = JSON.parse(itineraryDataString); 
                    console.log('Itinerary selected and parsed successfully:', selectedItineraryDetails); 
                    
                    if (selectedItineraryDetails && selectedItineraryDetails.shipName) {
                        reservationStatusDiv.innerHTML = `<p class="success">Selected Itinerary: ${selectedItineraryDetails.shipName} (Ref: ${itineraryId.substring(0, 15)}...). Please fill passenger/cabin details and click 'Make Reservation'.</p>`;
                    } else {
                        console.error("Parsed itinerary details object is missing shipName or is null/undefined.", selectedItineraryDetails);
                        displayError(reservationStatusDiv, "Error selecting itinerary: details incomplete after parsing.");
                        selectedItineraryDetails = null; 
                    }
                } catch (e) {
                    console.error("Error parsing itinerary details JSON:", e);
                    console.error("Problematic JSON string was:", itineraryDataString);
                    displayError(reservationStatusDiv, "Error selecting itinerary: could not parse details. Check console for problematic JSON.");
                    selectedItineraryDetails = null; 
                }
            });
        });
    }

    async function handleMakeReservation() {
        console.log('handleMakeReservation called. Current state of selectedItineraryDetails:', selectedItineraryDetails);
        reservationStatusDiv.innerHTML = ''; 

        const clientName = clientNameInput.value.trim();
        const numPassengers = parseInt(numPassengersInput.value, 10);
        const numCabins = parseInt(numCabinsInput.value, 10);

        if (!clientName) {
            displayError(reservationStatusDiv, 'Please enter your name.');
            return;
        }
        if (!selectedItineraryDetails || typeof selectedItineraryDetails !== 'object' || Object.keys(selectedItineraryDetails).length === 0) {
            console.error('Validation failed in handleMakeReservation: selectedItineraryDetails is not a valid object.', selectedItineraryDetails);
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
        console.log("Submitting reservationData:", reservationData);

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
            
            displaySuccess(reservationStatusDiv, `${responseData.message} Your Reservation ID is: <strong>${responseData.reservationId}</strong>`);
            
        } catch (error) {
            console.error('Error making reservation:', error);
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

        console.log(`Attempting to cancel reservation with ID: ${reservationId}`);

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
            console.error('Error cancelling reservation:', error);
            const displayErrorMessage = error.message || error.toString();
            displayError(reservationStatusDiv, `Cancellation failed: ${displayErrorMessage}`);
        }
    }

    function displayError(divElement, message) {
        divElement.innerHTML = `<p class="error">${message}</p>`;
    }
    
    function displaySuccess(divElement, message) {
        divElement.innerHTML = `<p class="success">${message}</p>`;
    }
});

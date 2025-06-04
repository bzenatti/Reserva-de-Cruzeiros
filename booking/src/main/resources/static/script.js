document.addEventListener('DOMContentLoaded', () => {
    // Get references to the HTML elements
    const destinationSelect = document.getElementById('destination');
    const departureDateInput = document.getElementById('departureDate');
    const embarkationPortInput = document.getElementById('embarkationPort');
    const searchItinerariesButton = document.getElementById('searchItinerariesButton');
    const itinerariesResultDiv = document.getElementById('itinerariesResult');
    const selectedItineraryIdInput = document.getElementById('selectedItineraryId'); // For reservation part

    // --- Function to search for available itineraries ---
    async function searchItineraries() {
        // Clear previous results and errors
        itinerariesResultDiv.innerHTML = '';
        selectedItineraryIdInput.value = ''; // Clear selected itinerary for reservation

        // Get input values
        const destination = destinationSelect.value;
        const departureDateValue = departureDateInput.value; // Format: "YYYY-MM"
        const embarkationPort = embarkationPortInput.value.trim();

        // Validate inputs
        if (!destination) {
            displayError('Please select a destination.');
            return;
        }
        if (!departureDateValue) {
            displayError('Please select a departure year and month.');
            return;
        }
        if (!embarkationPort) {
            displayError('Please enter an embarkation port.');
            return;
        }

        // Split departureDateValue into year and month
        const [yearStr, monthStr] = departureDateValue.split('-');
        const year = parseInt(yearStr, 10);
        const month = parseInt(monthStr, 10);

        // Prepare headers for the request
        const headers = new Headers();
        headers.append('destination', destination);
        headers.append('year', year.toString());
        headers.append('month', month.toString());
        headers.append('embarkationPort', embarkationPort);

        try {
            // Make the fetch request
            const response = await fetch('/available-itineraries', {
                method: 'GET',
                headers: headers
            });

            if (!response.ok) {
                // Handle HTTP errors
                const errorData = await response.text();
                throw new Error(`HTTP error ${response.status}: ${errorData || 'Failed to fetch itineraries'}`);
            }

            // Parse the JSON response
            const itineraries = await response.json();

            // Display the results
            displayItineraries(itineraries, year, month);

        } catch (error) {
            console.error('Error fetching itineraries:', error);
            displayError(`Error fetching itineraries: ${error.message}`);
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

            // Create a unique ID for this itinerary for selection purposes
            const itineraryId = `${itinerary.shipName}-${itinerary.departureDayOfMonth}-${selectedYear}-${selectedMonth}-${index}`;


            let departureDateStr = "N/A";
            try {
                const departureDate = new Date(selectedYear, selectedMonth - 1, itinerary.departureDayOfMonth);
                if (departureDate.getFullYear() === selectedYear && departureDate.getMonth() === selectedMonth -1) {
                     departureDateStr = departureDate.toLocaleDateString(); 
                } else {
                    departureDateStr = `Day ${itinerary.departureDayOfMonth} (check month/year validity)`;
                }

            } catch (e) {
                console.warn("Could not form valid date for itinerary:", itinerary, e);
            }


            li.innerHTML = `
                <h3>${itinerary.shipName} (ID: ${itineraryId.substring(0,15)}...)</h3>
                <p><strong>Departure Date:</strong> ${departureDateStr} (Departs on day ${itinerary.departureDayOfMonth} of the month)</p>
                <p><strong>Route:</strong> ${itinerary.embarkationPort} to ${itinerary.disembarkationPort}</p>
                <p><strong>Visits:</strong> ${itinerary.visitedPlaces ? itinerary.visitedPlaces.join(', ') : 'N/A'}</p>
                <p><strong>Nights:</strong> ${itinerary.nights}</p>
                <p><strong>Price per Person:</strong> $${itinerary.pricePerPerson ? itinerary.pricePerPerson.toFixed(2) : 'N/A'}</p>
                <p><strong>Cabins:</strong> ${itinerary.availableCabins} available / ${itinerary.maxCabins} total</p>
                <p><strong>Passengers:</strong> ${itinerary.availablePassengers} available / ${itinerary.maxPassengers} total</p>
                <button class="select-itinerary-button" data-itinerary-id="${itineraryId}" data-itinerary-details='${JSON.stringify(itinerary)}'>Select for Reservation</button>
            `;
            ul.appendChild(li);
        });

        itinerariesResultDiv.appendChild(ul);

        // Add event listeners to the "Select for Reservation" buttons
        document.querySelectorAll('.select-itinerary-button').forEach(button => {
            button.addEventListener('click', (event) => {
                const itineraryId = event.target.getAttribute('data-itinerary-id');
                selectedItineraryIdInput.value = itineraryId;
                // For now, just setting the ID.
                document.getElementById('reservationStatus').innerHTML = `<p class="success">Selected Itinerary: ${itineraryId.substring(0,25)}...</p>`;
            });
        });
    }

    function displayError(message) {
        itinerariesResultDiv.innerHTML = `<p class="error">${message}</p>`;
    }

    if (searchItinerariesButton) {
        searchItinerariesButton.addEventListener('click', searchItineraries);
    }
});
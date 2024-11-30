document.addEventListener("DOMContentLoaded", () => {
  const toggleBtn = document.getElementById("toggleBtn");
  const sidebar = document.getElementById("sidebar");
  const mainContent = document.getElementById("main-content");

  // Toggle sidebar visibility
  toggleBtn.addEventListener("click", () => {
    sidebar.style.display = sidebar.style.display === "none" ? "block" : "none";
    mainContent.style.marginLeft = sidebar.style.display === "none" ? "0" : "250px";
  });

  // API Calls for Parking System
  document.getElementById("viewAvailableSpaces").addEventListener("click", () => {
    fetchAvailableParkingSpaces();
  });

  document.getElementById("parkVehicle").addEventListener("click", () => {
    document.getElementById("parkVehicleForm").style.display = "block";
    document.getElementById("releaseVehicleForm").style.display = "none";
    document.getElementById("history").style.display = "none";
  });

  document.getElementById("releaseVehicle").addEventListener("click", () => {
    document.getElementById("releaseVehicleForm").style.display = "block";
    document.getElementById("parkVehicleForm").style.display = "none";
    document.getElementById("history").style.display = "none";
  });

  document.getElementById("viewHistory").addEventListener("click", () => {
    document.getElementById("history").style.display = "block";
    document.getElementById("parkVehicleForm").style.display = "none";
    document.getElementById("releaseVehicleForm").style.display = "none";
    fetchHistory();
  });

  // Fetch available parking spaces
  function fetchAvailableParkingSpaces() {
    fetch('/api/available-parking')
      .then(response => response.json())
      .then(data => {
        let table = '<table class="table"><thead><tr><th>Slot</th><th>Zone</th><th>Available</th></tr></thead><tbody>';
        data.forEach(space => {
          table += `<tr><td>${space.slot}</td><td>${space.zone}</td><td>${space.available}</td></tr>`;
        });
        table += '</tbody></table>';
        document.getElementById("dataTable").innerHTML = table;
      });
  }

  // Fetch parking history
  function fetchHistory() {
    fetch('/api/parking-history')
      .then(response => response.json())
      .then(data => {
        let historyTable = document.getElementById("historyTable").getElementsByTagName('tbody')[0];
        historyTable.innerHTML = '';
        data.forEach(record => {
          let row = historyTable.insertRow();
          row.insertCell(0).textContent = record.vehicleType;
          row.insertCell(1).textContent = record.slot;
          row.insertCell(2).textContent = record.zone;
          row.insertCell(3).textContent = record.status;
        });
      });
  }

  // Handle Park Vehicle form submission
  document.getElementById("parkForm").addEventListener("submit", function(event) {
    event.preventDefault();

    // Get form values
    const registrationNumber = document.getElementById("registrationNumber").value;
    const ownerName = document.getElementById("ownerName").value;
    const phoneNumber = document.getElementById("phoneNumber").value;
    const vehicleType = document.getElementById("vehicleType").value;
    const slot = document.getElementById("slot").value;
    const zone = document.getElementById("zone").value;

    // Create vehicle object
    const vehicle = {
      registrationNumber: registrationNumber,
      ownerName: ownerName,
      phoneNumber: phoneNumber,
      vehicleType: vehicleType,
      entryTime: new Date().toISOString(), // Assuming entryTime is set to current time
    };

    // Make API request to register vehicle
    fetch(`/api/register?zone=${zone}&slot=${slot}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(vehicle),
    })
    .then(response => response.json())
    .then(data => {
      if (data && data.message) {
        alert(data.message);
      } else {
        alert('Vehicle registered successfully!');
      }
    })
    .catch(error => {
      console.error('Error:', error);
      alert('Failed to register vehicle');
    });
  });

  // Handle Release Vehicle action
  document.getElementById("confirmExit").addEventListener("click", () => {
    const confirmExit = confirm("Are you sure you want to release the vehicle?");
    if (confirmExit) {
      fetch('/api/exit-vehicle')
        .then(response => response.json())
        .then(data => {
          alert(data.message);
        });
    }
  });
});

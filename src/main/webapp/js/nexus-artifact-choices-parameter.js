function objectToUrlFormEncoded(parameters) {
    // https://stackoverflow.com/a/37562814/4951015
    // Code could be simplified if support for HTMLUnit is dropped
    // body: new URLSearchParams(parameters) is enough then, but it doesn't work in HTMLUnit currently
    let formBody = [];
    for (const property in parameters) {
        const encodedKey = encodeURIComponent(property);
        const encodedValue = encodeURIComponent(parameters[property]);
        formBody.push(encodedKey + "=" + encodedValue);
    }
    return formBody.join("&");
}

function filterVersionOptions(e) {
    if (e.key === 'Enter') {
        const searchInput = e.currentTarget;
        const BASE_URL = document.getElementById("nexusArtifactChoiceBaseUrl").value;
        let serverId = document.getElementById("nexusArtifactChoiceServerId").value;
        let repository = document.getElementById("nexusArtifactChoiceRepository").value;
        let limits = document.getElementById("nexusArtifactChoiceMaxVersionCount").value;
        const selectElement = searchInput.previousElementSibling;
        const option = selectElement.getAttribute("group-artifact-id");
        selectElement.disabled = true
        fetch(BASE_URL + "/fillVersionOptionsItems", {
            method: "post",
            headers: crumb.wrap({
                "Content-Type": "application/x-www-form-urlencoded"
            }),
            body: objectToUrlFormEncoded({
                serverId: serverId,
                repository: repository,
                option: option,
                limits: limits,
                keyword: searchInput.value,
            })
        }).then(response => response.json())
            .then(resp => {
                selectElement.innerHTML = ''
                resp.values.forEach(subOption => {
                    let optionElement = document.createElement('option')
                    optionElement.value = subOption.value
                    optionElement.textContent = subOption.name
                    selectElement.appendChild(optionElement)
                });
            })
            .finally(() => selectElement.disabled = false);
        e.preventDefault();
    }
}

function updateVersionOptions(select) {
    const BASE_URL = document.getElementById("nexusArtifactChoiceBaseUrl").value;
    let serverId = document.getElementById("nexusArtifactChoiceServerId").value;
    let repository = document.getElementById("nexusArtifactChoiceRepository").value;
    let limits = document.getElementById("nexusArtifactChoiceMaxVersionCount").value;

    let selectedOptions = Array.from(select.selectedOptions).map(e => e.value)
    let container = document.getElementById('versionsContainer')
    container.innerHTML = ''
    select.disabled = true
    let promises = []
    for (let i = 0; i < selectedOptions.length; i++) {
        const option = selectedOptions.at(i)
        const selectName = option.replaceAll(".", "-").replaceAll(":", "-")
        const promise = fetch(BASE_URL + "/fillVersionOptionsItems", {
            method: "post",
            headers: crumb.wrap({
                "Content-Type": "application/x-www-form-urlencoded"
            }),
            body: objectToUrlFormEncoded({
                serverId: serverId,
                repository: repository,
                option: option,
                limits: limits
            })
        }).then(response => response.json())
            .then(resp => {
                if (resp.values.length > 0) {
                    let selectSearchDiv = document.createElement('div')
                    selectSearchDiv.style.marginBottom = '5px'
                    selectSearchDiv.style.padding = '1px'

                    let selectElement = document.createElement('select')
                    resp.values.forEach(subOption => {
                        let optionElement = document.createElement('option')
                        optionElement.value = subOption.value
                        optionElement.textContent = subOption.name
                        selectElement.appendChild(optionElement)
                    });
                    selectElement.name = selectName
                    selectElement.style.marginBottom = '7px'
                    selectElement.style.padding = '1px'
                    selectElement.setAttribute("group-artifact-id", option)

                    let searchInput = document.createElement('input')
                    searchInput.type = 'text'
                    searchInput.style.marginLeft = '5px'
                    searchInput.addEventListener("keydown", filterVersionOptions)

                    selectSearchDiv.appendChild(selectElement)
                    selectSearchDiv.appendChild(searchInput)

                    container.appendChild(selectSearchDiv)
                }
            })
        promises.push(promise)
    }

    Promise.all(promises).finally(() => {
        select.disabled = false
    })
}

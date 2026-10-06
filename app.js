const $ = s => document.querySelector(s);
const $$ = s => document.querySelectorAll(s);

let data = {};

let session = JSON.parse(
    localStorage.getItem('resqsimSession') || 'null'
);


/* =====================================================
   API
===================================================== */

async function api(url, opt = {}) {

    const r = await fetch('/api' + url, {

        headers: {
            'Content-Type': 'application/json',
            ...(opt.headers || {})
        },

        ...opt
    });

    let body = {};

    try {
        body = await r.json();
    } catch (_) {}

    if (!r.ok) {
        throw Error(
            body.message || 'Request failed'
        );
    }

    return body;
}


/* =====================================================
   TOAST
===================================================== */

function toast(message) {

    const x = $('#toast');

    if (!x) {
        alert(message);
        return;
    }

    x.textContent = message;
    x.style.display = 'block';

    setTimeout(() => {
        x.style.display = 'none';
    }, 2800);
}


/* =====================================================
   MODALS
===================================================== */

function openModal(id) {

    const element = $('#' + id);

    if (element) {
        element.classList.add('show');
    }
}


function closeModal(id) {

    const element = $('#' + id);

    if (element) {
        element.classList.remove('show');
    }
}


/* =====================================================
   LOGIN TABS
===================================================== */

$$('.login-tab').forEach(button => {

    button.onclick = () => {

        $$('.login-tab').forEach(x => {
            x.classList.remove('active');
        });

        button.classList.add('active');

        const admin =
            button.dataset.role === 'admin';

        $('#adminLogin')
            .classList
            .toggle('hidden', !admin);

        $('#victimLogin')
            .classList
            .toggle('hidden', admin);
    };

});


/* =====================================================
   ADMIN LOGIN
===================================================== */

const adminLoginForm = $('#adminLogin');

if (adminLoginForm) {

    adminLoginForm.onsubmit = async e => {

        e.preventDefault();

        const form = new FormData(e.target);

        await doLogin({

            role: 'admin',

            email:
                form.get('email'),

            password:
                form.get('password')

        });
    };
}


/* =====================================================
   VICTIM LOGIN
===================================================== */

const victimLoginForm = $('#victimLogin');

if (victimLoginForm) {

    victimLoginForm.onsubmit = async e => {

        e.preventDefault();

        const form = new FormData(e.target);

        await doLogin({

            role: 'victim',

            victimId:
                Number(
                    form.get('victimId')
                ),

            phone:
                form.get('phone')

        });
    };
}


/* =====================================================
   LOGIN
===================================================== */

async function doLogin(payload) {

    try {

        const response =
            await api('/login', {

                method: 'POST',

                body:
                    JSON.stringify(payload)

            });

        session = response;

        localStorage.setItem(
            'resqsimSession',
            JSON.stringify(response)
        );

        showApp();

        toast(
            'Signed in successfully'
        );

    } catch (error) {

        toast(error.message);

        console.error(
            'Login error:',
            error
        );
    }
}


/* =====================================================
   SHOW APPLICATION
===================================================== */

function showApp() {

    if (!session) {
        return;
    }

    const loginScreen =
        $('#loginScreen');

    if (loginScreen) {
        loginScreen.classList.add('hidden');
    }


    if (session.role === 'admin') {

        $('#adminApp')
            .classList
            .remove('hidden');

        $('#victimApp')
            .classList
            .add('hidden');

        loadAll();

    } else {

        $('#victimApp')
            .classList
            .remove('hidden');

        $('#adminApp')
            .classList
            .add('hidden');

        loadVictimPortal();
    }
}


/* =====================================================
   LOGOUT
===================================================== */

function logout() {

    session = null;

    localStorage.removeItem(
        'resqsimSession'
    );

    $('#adminApp')
        .classList
        .add('hidden');

    $('#victimApp')
        .classList
        .add('hidden');

    $('#loginScreen')
        .classList
        .remove('hidden');
}


/* =====================================================
   PAGE LABELS
===================================================== */

const labels = {

    dashboard: [
        'Operations Dashboard',
        'Live disaster response overview'
    ],

    victims: [
        'Victim Management',
        'Registered people and current response status'
    ],

    sos: [
        'Emergency / SOS Queue',
        'Prioritize and dispatch help'
    ],

    teams: [
        'Rescue Teams',
        'Dispatch teams to active emergencies'
    ],

    volunteers: [
        'Volunteers',
        'People and skills supporting response'
    ],

    shelters: [
        'Shelters',
        'Emergency accommodation capacity'
    ],

    resources: [
        'Resources',
        'Supply requests and allocation'
    ],

    scheduling: [
        'OS Scheduling Lab',
        'FCFS and Priority scheduling'
    ],

    reports: [
        'Response Report',
        'Operational summary'
    ]
};


/* =====================================================
   NAVIGATION
===================================================== */

function go(page) {

    $$('.page').forEach(x => {
        x.classList.remove('active');
    });

    const target =
        $('#' + page);

    if (target) {
        target.classList.add('active');
    }

    $$('.nav').forEach(x => {

        x.classList.toggle(
            'active',
            x.dataset.page === page
        );

    });

    if (labels[page]) {

        $('#pageTitle').textContent =
            labels[page][0];

        $('#pageSub').textContent =
            labels[page][1];
    }

    if (page === 'reports') {
        loadReport();
    }
}


$$('.nav').forEach(button => {

    button.onclick = () => {
        go(button.dataset.page);
    };

});


/* =====================================================
   LOAD ADMIN DATA
===================================================== */

async function loadAll() {

    try {

        const [
            dash,
            victims,
            sos,
            teams,
            vols,
            shelters,
            resources,
            inventory
        ] = await Promise.all([

            api('/dashboard'),

            api('/victims'),

            api('/admin/sos'),

            api('/teams'),

            api('/volunteers'),

            api('/shelters'),

            api('/admin/resources'),

            api('/resources/inventory')
        ]);


        data = {

            victims,

            teams,

            sos,

            vols,

            shelters,

            res: resources,

            inventory

        };


        renderDash(dash);

        renderVictims(victims);

        renderSos(sos);

        renderTeams(teams);

        renderVols(vols);

        renderShelters(shelters);

        renderResources(resources);

        renderInventory(inventory);

        fillSelects(
            shelters,
            teams
        );


        const health =
            await api('/health');


        $('#dbState').textContent =
            health.ok
                ? '● MySQL connected'
                : '● Database error';


        $('#dbState').className =
            'db-pill ' +
            (
                health.ok
                    ? 'ok'
                    : 'bad'
            );

    } catch (error) {

        console.error(
            'Load error:',
            error
        );

        if ($('#dbState')) {

            $('#dbState').textContent =
                '● Database offline';

            $('#dbState').className =
                'db-pill bad';
        }

        toast(
            'Could not load data: ' +
            error.message
        );
    }
}


/* =====================================================
   DASHBOARD
===================================================== */

function renderDash(d) {

    $('#heroSos').textContent =
        d.pendingSos;


    $('#stats').innerHTML = [

        ['Victims', d.victims, '👥'],

        ['Pending SOS', d.pendingSos, '🚨'],

        ['Assigned teams', d.activeTeams, '🚑'],

        ['Available beds', d.availableBeds, '🏠']

    ].map(x => `

        <div class="stat">

            <span>${x[2]}</span>

            <small>${x[0]}</small>

            <b>${x[1]}</b>

        </div>

    `).join('');


    $('#dashSos').innerHTML =

        (data.sos || [])

            .slice(0, 5)

            .map(x => `

                <div class="alert-row">

                    <div>

                        <b>${escapeHtml(x.name)}</b>

                        <small>

                            ${escapeHtml(
                                x.location ||
                                'Location pending'
                            )}

                            ·

                            ${escapeHtml(
                                x.emergency_type ||
                                'Emergency'
                            )}

                        </small>

                    </div>

                    <span class="badge red">

                        Severity ${x.severity}

                    </span>

                </div>

            `).join('')

        ||

        '<p class="muted">No pending SOS requests.</p>';


    const total =
        (
            d.availableTeams +
            d.activeTeams
        ) || 1;


    $('#capacity').innerHTML = `

        <div class="metric">

            <div>

                <span>
                    Teams available
                </span>

                <b>
                    ${d.availableTeams}
                </b>

            </div>

            <div class="mini-progress">

                <span
                    style="width:${
                        d.availableTeams /
                        total *
                        100
                    }%">
                </span>

            </div>

        </div>


        <div class="metric">

            <div>

                <span>
                    Volunteers registered
                </span>

                <b>
                    ${d.volunteers}
                </b>

            </div>

        </div>

    `;
}


/* =====================================================
   BADGE
===================================================== */

function badge(status) {

    const className =

        status?.includes('Pending') ||
        status?.includes('Severity 5')

            ? 'red'

            : status?.includes('Allocated') ||
              status === 'Approved' ||
              status === 'Available'

                ? 'green'

                : 'blue';


    return `

        <span class="badge ${className}">

            ${escapeHtml(status || '—')}

        </span>

    `;
}


/* =====================================================
   VICTIMS
===================================================== */

function renderVictims(a) {

    $('#victimRows').innerHTML =

        a.map(x => `

            <tr>

                <td>
                    #${x.victim_id}
                </td>

                <td>
                    <b>${escapeHtml(x.name)}</b>
                </td>

                <td>
                    ${escapeHtml(x.phone || '—')}
                </td>

                <td>
                    ${escapeHtml(x.location || '—')}
                </td>

                <td>
                    ${escapeHtml(
                        x.emergency_type || '—'
                    )}
                </td>

                <td>
                    ${x.severity || '—'}
                </td>

                <td>
                    ${badge(x.status)}
                </td>

                <td>

                    ${
                        x.team_id

                        ? `

                            <div class="team-cell">

                                <b>
                                    #${x.team_id}
                                </b>

                                <span>
                                    ${escapeHtml(
                                        x.team_name ||
                                        'Rescue Team'
                                    )}
                                </span>

                            </div>

                          `

                        : `

                            <span class="muted">
                                Not Assigned
                            </span>

                          `
                    }

                </td>

                <td>

                    <button
                        class="ghost small"
                        onclick="openSos(
                            ${x.victim_id}
                        )">

                        SOS

                    </button>


                    <button
                        class="ghost small"
                        onclick="requestResource(
                            ${x.victim_id}
                        )">

                        Resource

                    </button>

                </td>

            </tr>

        `).join('')

        ||

        `

            <tr>

                <td colspan="9">
                    No victims yet.
                </td>

            </tr>

        `;
}


/* =====================================================
   SOS
===================================================== */

function renderSos(a) {

    $('#sosRows').innerHTML =

        a.map(x => `

            <tr>

                <td>
                    #${x.victim_id}
                </td>

                <td>

                    <b>
                        ${escapeHtml(x.name)}
                    </b>

                    <small>
                        ${escapeHtml(x.phone || '')}
                    </small>

                </td>

                <td>
                    ${escapeHtml(x.location || '—')}
                </td>

                <td>
                    ${escapeHtml(
                        x.emergency_type || '—'
                    )}
                </td>

                <td>
                    ${badge(
                        'Severity ' +
                        x.severity
                    )}
                </td>

                <td>

                    ${
                        x.team_id
                            ? badge('Assigned')
                            : badge('Pending')
                    }

                </td>

                <td>

                    <button
                        class="ghost small"
                        onclick="openTeam(
                            ${x.victim_id}
                        )">

                        Assign team

                    </button>

                </td>

            </tr>

        `).join('')

        ||

        `

            <tr>

                <td colspan="7">
                    No pending SOS requests.
                </td>

            </tr>

        `;
}


/* =====================================================
   RESCUE TEAMS
   UPDATED:
   Displays the victim_id assigned to each team
===================================================== */

function renderTeams(a) {

    $('#teamCards').innerHTML =

        a.map(x => `

            <div class="card team">

                <div class="team-icon">
                    🚑
                </div>

                <h3>
                    #${x.team_id}
                    ·
                    ${escapeHtml(
                        x.team_name ||
                        'Rescue Team'
                    )}
                </h3>

                <p>

                    <b>Leader:</b>
                    ${escapeHtml(
                        x.leader || '—'
                    )}

                </p>

                <p>

                    <b>Vehicle:</b>
                    ${escapeHtml(
                        x.vehicle || '—'
                    )}

                </p>

                ${badge(x.rescue_status)}


                ${
                    x.victim_id

                        ? `

                            <div
                                class="assigned-victim"
                                style="
                                    margin-top:12px;
                                    padding:10px;
                                    border-radius:8px;
                                    background:rgba(0,0,0,0.04);
                                "
                            >

                                <strong>
                                    Assigned Victim ID:
                                </strong>

                                <span>
                                    #${x.victim_id}
                                </span>

                            </div>

                          `

                        : `

                            <p class="muted">
                                No victim assigned
                            </p>

                          `
                }


                ${
                    x.rescue_status ===
                    'Available'

                        ? `

                            <p class="muted">
                                Ready for dispatch
                            </p>

                          `

                        : `

                            <p class="muted">
                                Currently assigned
                            </p>

                          `
                }

            </div>

        `).join('')

        ||

        `

            <div class="card">

                <p>
                    No rescue teams registered.
                </p>

            </div>

        `;
}


/* =====================================================
   VOLUNTEERS
===================================================== */

function renderVols(a) {

    $('#volRows').innerHTML =

        a.map(x => `

            <tr>

                <td>
                    <b>${escapeHtml(x.name)}</b>
                </td>

                <td>
                    ${escapeHtml(x.phone || '—')}
                </td>

                <td>
                    ${escapeHtml(x.skill || '—')}
                </td>

                <td>
                    ${escapeHtml(
                        x.shelter_name || '—'
                    )}
                </td>

                <td>
                    ${badge(x.availability)}
                </td>

                <td>

                    <button
                        class="ghost small"
                        onclick="toggleVol(
                            ${x.volunteer_id},
                            '${x.availability ===
                            'Available'
                                ? 'Unavailable'
                                : 'Available'}'
                        )">

                        Toggle

                    </button>

                </td>

            </tr>

        `).join('')

        ||

        `

            <tr>

                <td colspan="6">
                    No volunteers yet.
                </td>

            </tr>

        `;
}


/* =====================================================
   SHELTERS
===================================================== */

function renderShelters(a) {

    $('#shelterCards').innerHTML =

        a.map(x => `

            <div class="card shelter">

                <div class="shelter-top">

                    <div class="shelter-icon">
                        🏠
                    </div>

                    ${badge(
                        x.available_beds > 0
                            ? 'Available'
                            : 'Full'
                    )}

                </div>

                <h3>
                    ${escapeHtml(x.shelter_name)}
                </h3>

                <p>
                    ${escapeHtml(x.address || '—')}
                </p>

                <div class="bed-number">

                    <b>
                        ${x.available_beds}
                    </b>

                    <span>
                        available beds of
                        ${x.capacity}
                    </span>

                </div>

                <div class="progress">

                    <span
                        style="width:${
                            Math.min(
                                100,
                                x.available_beds /
                                x.capacity *
                                100
                            )
                        }%">
                    </span>

                </div>

                <button
                    class="ghost"
                    onclick="assignShelter(
                        ${x.shelter_id}
                    )">

                    Assign to victim

                </button>

            </div>

        `).join('')

        ||

        `

            <div class="card">

                <p>
                    No shelters available.
                </p>

            </div>

        `;
}


/* =====================================================
   RESOURCE REQUESTS
===================================================== */

function renderResources(a) {

    $('#resourceRows').innerHTML =

        a.map(x => {

            let resourceName =
                x.resource_request || '—';

            let quantity = 1;

            if (
                resourceName.includes(':')
            ) {

                const parts =
                    resourceName.split(':');

                resourceName =
                    parts[0].trim();

                const parsedQuantity =
                    Number(
                        parts[1].trim()
                    );

                if (
                    Number.isFinite(
                        parsedQuantity
                    )
                ) {

                    quantity =
                        parsedQuantity;
                }
            }


            return `

                <tr>

                    <td>

                        #${x.victim_id}

                        <b>
                            ${escapeHtml(x.name)}
                        </b>

                    </td>

                    <td>
                        ${escapeHtml(resourceName)}
                    </td>

                    <td>
                        ${quantity}
                    </td>

                    <td>
                        ${badge(
                            x.resource_status
                        )}
                    </td>

                    <td>

                        <button
                            class="ghost small"
                            onclick="allocate(
                                ${x.victim_id}
                            )">

                            Allocate

                        </button>

                    </td>

                </tr>

            `;

        }).join('')

        ||

        `

            <tr>

                <td colspan="5">
                    No pending resource requests.
                </td>

            </tr>

        `;
}


/* =====================================================
   RESOURCE INVENTORY
===================================================== */

function renderInventory(a) {

    $('#inventoryRows').innerHTML =

        a.map(x => `

            <tr>

                <td>
                    <b>
                        ${escapeHtml(
                            x.resource_name
                        )}
                    </b>
                </td>

                <td>
                    ${x.total_quantity}
                </td>

                <td>
                    ${x.allocated_quantity}
                </td>

                <td>
                    <b>
                        ${x.remaining_quantity}
                    </b>
                </td>

                <td>

                    <button
                        class="ghost small"
                        onclick="addStock(
                            ${x.resource_id},
                            '${String(
                                x.resource_name
                            ).replace(
                                /'/g,
                                "\\'"
                            )}'
                        )">

                        Add Stock

                    </button>

                </td>

            </tr>

        `).join('')

        ||

        `

            <tr>

                <td colspan="5">
                    No resources in inventory.
                </td>

            </tr>

        `;
}


/* =====================================================
   ADD INVENTORY RESOURCE
===================================================== */

async function addInventoryResource() {

    const resourceName =
        prompt(
            'Enter resource name:'
        );

    if (!resourceName) {
        return;
    }


    const quantity =
        Number(
            prompt(
                'Enter total quantity:'
            )
        );


    if (
        !Number.isFinite(quantity) ||
        quantity <= 0
    ) {

        toast(
            'Enter a valid quantity'
        );

        return;
    }


    try {

        await api(
            '/admin/resources/inventory',
            {

                method: 'POST',

                body: JSON.stringify({

                    resourceName:
                        resourceName,

                    quantity:
                        quantity

                })

            }
        );


        toast(
            'Resource added'
        );

        loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   ADD STOCK
===================================================== */

async function addStock(id, name) {

    const quantity =
        Number(
            prompt(
                'Add quantity for ' +
                name +
                ':'
            )
        );


    if (
        !Number.isFinite(quantity) ||
        quantity <= 0
    ) {

        toast(
            'Enter a valid quantity'
        );

        return;
    }


    try {

        await api(

            '/admin/resources/inventory/' +
            id +
            '/add-stock',

            {

                method: 'PUT',

                body: JSON.stringify({

                    quantity:
                        quantity

                })

            }

        );


        toast(
            'Stock added'
        );

        loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   FILL SELECTS
===================================================== */

function fillSelects(
    shelters,
    teams
) {

    $('#teamSelect').innerHTML =

        '<option value="">Select available rescue team</option>' +

        teams
            .filter(
                x =>
                    x.rescue_status ===
                    'Available'
            )
            .map(x => `

                <option
                    value="${x.team_id}">

                    #${x.team_id}
                    ${escapeHtml(x.team_name)}
                    —
                    ${escapeHtml(x.leader || '')}
                    —
                    ${escapeHtml(x.vehicle || '')}

                </option>

            `)
            .join('');


    $('#volShelter').innerHTML =

        '<option value="0">No shelter</option>' +

        shelters
            .map(x => `

                <option
                    value="${x.shelter_id}">

                    ${escapeHtml(
                        x.shelter_name
                    )}

                </option>

            `)
            .join('');
}


/* =====================================================
   REGISTER VICTIM
===================================================== */

async function registerVictim(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);


        const response =
            await api(
                '/victims',
                {

                    method: 'POST',

                    body: JSON.stringify(
                        Object.fromEntries(form)
                    )

                }
            );


        closeModal(
            'victimModal'
        );

        e.target.reset();


        toast(

            response.message +

            ` — Victim ID ${
                response.victimId
            }`

        );


        loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   OPEN SOS
===================================================== */

function openSos(id) {

    $('#sosModal input[name=id]')
        .value = id;

    openModal(
        'sosModal'
    );
}


/* =====================================================
   CREATE SOS
===================================================== */

async function createSos(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);

        const id =
            form.get('id');


        await api(

            `/victims/${id}/sos`,

            {

                method: 'PUT',

                body: JSON.stringify(
                    Object.fromEntries(form)
                )

            }

        );


        closeModal(
            'sosModal'
        );


        e.target.reset();


        toast(
            'SOS created successfully'
        );


        loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   OPEN TEAM
===================================================== */

function openTeam(id) {

    $('#teamModal input[name=victimId]')
        .value = id;

    openModal(
        'teamModal'
    );
}


/* =====================================================
   ASSIGN TEAM
===================================================== */

async function assignTeam(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);

        const object =
            Object.fromEntries(form);


        object.victimId =
            Number(
                object.victimId
            );


        object.teamId =
            Number(
                object.teamId
            );


        const response =
            await api(
                '/admin/assign-team',
                {

                    method: 'PUT',

                    body:
                        JSON.stringify(object)

                }
            );


        closeModal(
            'teamModal'
        );


        toast(

            response.message ||
            'Rescue team assigned'

        );


        loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   REGISTER RESCUE TEAM
===================================================== */

async function registerRescueTeam(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);

        const object =
            Object.fromEntries(form);


        object.teamId =
            Number(
                object.teamId
            );


        const response =
            await api(
                '/admin/teams',
                {

                    method: 'POST',

                    body:
                        JSON.stringify(object)

                }
            );


        closeModal(
            'rescueTeamModal'
        );


        e.target.reset();


        toast(

            response.message ||
            'Rescue team registered'

        );


        loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   REQUEST RESOURCE
   ADMIN VICTIM MANAGEMENT

   Opens the resource modal instead of prompt().
===================================================== */

async function requestResource(id) {

    try {

        const inventory =
            await api(
                '/resources/inventory/available'
            );


        if (!inventory.length) {

            toast(
                'No resources are currently available'
            );

            return;
        }


        const select =
            $('#adminResourceSelect');


        if (!select) {

            toast(
                'Resource request form not found'
            );

            return;
        }


        select.innerHTML =

            '<option value="">Select resource</option>' +

            inventory
                .map(x => `

                    <option
                        value="${escapeHtml(
                            x.resource_name
                        )}"
                        data-available="${
                            x.remaining_quantity
                        }">

                        ${escapeHtml(
                            x.resource_name
                        )}

                        —
                        ${x.remaining_quantity}
                        available

                    </option>

                `)
                .join('');


        const victimIdInput =
            $('#resourceRequestModal input[name=victimId]');


        if (victimIdInput) {

            victimIdInput.value =
                id;
        }


        const quantityInput =
            $('#resourceRequestModal input[name=quantity]');


        if (quantityInput) {

            quantityInput.value = '';

            quantityInput.max = '';
        }


        openModal(
            'resourceRequestModal'
        );


    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   ADMIN RESOURCE REQUEST SUBMISSION
===================================================== */

async function submitAdminResourceRequest(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);


        const victimId =
            Number(
                form.get('victimId')
            );


        const resource =
            form.get('resource');


        const quantity =
            Number(
                form.get('quantity')
            );


        if (!victimId) {

            toast(
                'Invalid victim'
            );

            return;
        }


        if (!resource) {

            toast(
                'Please select a resource'
            );

            return;
        }


        if (
            !Number.isInteger(quantity) ||
            quantity <= 0
        ) {

            toast(
                'Please enter a valid quantity'
            );

            return;
        }


        const select =
            $('#adminResourceSelect');


        const selectedOption =
            select.options[
                select.selectedIndex
            ];


        const available =
            Number(
                selectedOption?.dataset?.available
            );


        if (
            Number.isFinite(available) &&
            quantity > available
        ) {

            toast(
                `Only ${available} units are available`
            );

            return;
        }


        await api(

            `/victims/${victimId}/resource`,

            {

                method: 'PUT',

                body: JSON.stringify({

                    resource:
                        resource,

                    quantity:
                        quantity

                })

            }

        );


        closeModal(
            'resourceRequestModal'
        );


        e.target.reset();


        toast(
            'Resource request sent successfully'
        );


        loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   ALLOCATE REQUESTED RESOURCE
===================================================== */

async function allocate(id) {

    try {

        await api(

            '/admin/resources/' +
            id,

            {

                method: 'PUT'

            }

        );


        toast(
            'Resources allocated successfully'
        );


        loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   VOLUNTEER
===================================================== */

async function addVolunteer(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);

        const object =
            Object.fromEntries(form);


        object.shelterId =
            Number(
                object.shelterId
            ) || null;


        await api(
            '/volunteers',
            {

                method: 'POST',

                body:
                    JSON.stringify(object)

            }
        );


        closeModal(
            'volunteerModal'
        );


        e.target.reset();


        toast(
            'Volunteer added'
        );


        loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   TOGGLE VOLUNTEER
===================================================== */

async function toggleVol(
    id,
    availability
) {

    try {

        await api(

            `/volunteers/${id}/availability`,

            {

                method: 'PUT',

                body: JSON.stringify({

                    availability:
                        availability

                })

            }

        );


        loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   ASSIGN SHELTER
===================================================== */

async function assignShelter(id) {

    const victim =
        prompt(
            'Enter Victim ID to assign this shelter:'
        );


    if (!victim) {
        return;
    }


    try {

        await api(
            '/admin/shelter',
            {

                method: 'PUT',

                body: JSON.stringify({

                    victimId:
                        Number(victim),

                    shelterId:
                        id

                })

            }
        );


        toast(
            'Shelter assigned'
        );


        loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   REPORT
===================================================== */

async function loadReport() {

    try {

        const report =
            await api(
                '/report'
            );


        $('#reportRows').innerHTML =

            report.map(x => `

                <tr>

                    <td>
                        #${x.victim_id}
                        ${escapeHtml(x.name)}
                    </td>

                    <td>
                        ${escapeHtml(
                            x.location || '—'
                        )}
                    </td>

                    <td>
                        ${escapeHtml(
                            x.emergency_type || '—'
                        )}
                    </td>

                    <td>
                        ${x.severity || '—'}
                    </td>

                    <td>
                        ${badge(x.status)}
                    </td>

                    <td>
                        ${escapeHtml(
                            x.team_name || '—'
                        )}
                    </td>

                    <td>
                        ${escapeHtml(
                            x.shelter_name || '—'
                        )}
                    </td>

                    <td>
                        ${escapeHtml(
                            x.resource_status || '—'
                        )}
                    </td>

                </tr>

            `).join('');


    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   SCHEDULING
===================================================== */

async function runSchedule(type) {

    try {

        const inputId =
            type === 'fcfs'
                ? 'fcfsInput'
                : 'priorityInput';


        const raw =
            $('#' + inputId)
                .value
                .trim()
                .split('\n')
                .filter(Boolean);


        const jobs =
            raw.map(line => {

                const parts =
                    line
                        .split(',')
                        .map(x =>
                            x.trim()
                        );


                if (type === 'fcfs') {

                    return {

                        id:
                            parts[0],

                        arrival:
                            Number(parts[1]),

                        burst:
                            Number(parts[2])

                    };
                }


                return {

                    id:
                        parts[0],

                    burst:
                        Number(parts[1]),

                    priority:
                        Number(parts[2])

                };
            });


        const response =
            await api(

                '/scheduling/' +
                type,

                {

                    method: 'POST',

                    body:
                        JSON.stringify(jobs)

                }

            );


        $('#scheduleResult').innerHTML = `

            <h3>
                Scheduling result
            </h3>

            <div class="schedule-metrics">

                <span>

                    Average waiting

                    <b>
                        ${
                            response.avgWaiting
                                .toFixed(2)
                        }
                    </b>

                </span>


                <span>

                    Average turnaround

                    <b>
                        ${
                            response.avgTurnaround
                                .toFixed(2)
                        }
                    </b>

                </span>

            </div>


            <div class="tablewrap">

                <table>

                    <thead>

                        <tr>

                            ${
                                Object.keys(
                                    response.jobs[0] || {}
                                )
                                .map(key => `

                                    <th>
                                        ${escapeHtml(key)}
                                    </th>

                                `)
                                .join('')
                            }

                        </tr>

                    </thead>


                    <tbody>

                        ${
                            response.jobs

                                .map(job => `

                                    <tr>

                                        ${
                                            Object.values(job)
                                                .map(value => `

                                                    <td>
                                                        ${escapeHtml(
                                                            value
                                                        )}
                                                    </td>

                                                `)
                                                .join('')
                                        }

                                    </tr>

                                `)
                                .join('')
                        }

                    </tbody>

                </table>

            </div>

        `;

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   VICTIM PORTAL
===================================================== */

async function loadVictimPortal() {

    try {

        const id =
            session.user.victim_id;


        const [
            victim,
            shelters
        ] = await Promise.all([

            api(
                '/victim/' +
                id +
                '/full'
            ),

            api(
                '/shelters'
            )

        ]);


        session.user =
            victim;


        localStorage.setItem(

            'resqsimSession',

            JSON.stringify(session)

        );


        $('#vName').textContent =
            'Hello, ' +
            victim.name;


        $('#vStatus').textContent =
            victim.status ||
            'Registered';


        $('#victimStatusCard').innerHTML = `

            <div class="status-list">

                <div>

                    <span>
                        Emergency
                    </span>

                    <b>
                        ${
                            escapeHtml(
                                victim.emergency_type ||
                                'No SOS yet'
                            )
                        }
                    </b>

                </div>


                <div>

                    <span>
                        Severity
                    </span>

                    <b>
                        ${
                            victim.severity ||
                            '—'
                        }
                    </b>

                </div>


                <div>

                    <span>
                        Rescue team
                    </span>

                    <b>
                        ${
                            escapeHtml(
                                victim.team_name ||
                                'Not assigned'
                            )
                        }
                    </b>

                </div>


                <div>

                    <span>
                        Shelter
                    </span>

                    <b>
                        ${
                            escapeHtml(
                                victim.shelter_name ||
                                'Not assigned'
                            )
                        }
                    </b>

                </div>


                <div>

                    <span>
                        Resources
                    </span>

                    <b>
                        ${
                            escapeHtml(
                                victim.resource_status ||
                                'No request'
                            )
                        }
                    </b>

                </div>

            </div>

        `;


        $('#victimShelters').innerHTML =

            shelters
                .slice(0, 4)
                .map(s => `

                    <div class="shelter-mini">

                        <b>
                            ${escapeHtml(
                                s.shelter_name
                            )}
                        </b>

                        <span>
                            ${s.available_beds}
                            beds available
                        </span>

                    </div>

                `)
                .join('')

            ||

            `

                <p class="muted">

                    No shelter currently
                    has capacity.

                </p>

            `;


        await loadVictimResourceOptions();


    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   LOAD VICTIM RESOURCE OPTIONS
===================================================== */

async function loadVictimResourceOptions() {

    const resourceSelect =
        $('#resourceForm select[name="resource"]');


    if (!resourceSelect) {
        return;
    }


    try {

        const inventory =
            await api(
                '/resources/inventory/available'
            );


        resourceSelect.innerHTML =

            '<option value="">Select resource</option>' +

            inventory
                .map(x => `

                    <option
                        value="${escapeHtml(
                            x.resource_name
                        )}"
                        data-available="${
                            x.remaining_quantity
                        }">

                        ${escapeHtml(
                            x.resource_name
                        )}

                        —
                        ${x.remaining_quantity}
                        available

                    </option>

                `)
                .join('');


        if (!inventory.length) {

            resourceSelect.innerHTML =
                '<option value="">No resources available</option>';
        }


    } catch (error) {

        console.error(
            'Could not load resources:',
            error
        );


        resourceSelect.innerHTML =
            '<option value="">Unable to load resources</option>';
    }
}


/* =====================================================
   VICTIM RESOURCE FORM
===================================================== */

const resourceForm =
    $('#resourceForm');


if (resourceForm) {

    resourceForm.onsubmit =
        async e => {

            e.preventDefault();

            try {

                const form =
                    new FormData(
                        e.target
                    );


                const resource =
                    form.get('resource');


                const quantity =
                    Number(
                        form.get('quantity')
                    );


                if (!resource) {

                    toast(
                        'Please select a resource'
                    );

                    return;
                }


                if (
                    !Number.isInteger(quantity) ||
                    quantity <= 0
                ) {

                    toast(
                        'Please enter a valid quantity'
                    );

                    return;
                }


                const select =
                    e.target.querySelector(
                        'select[name="resource"]'
                    );


                const selectedOption =
                    select?.options[
                        select.selectedIndex
                    ];


                const available =
                    Number(
                        selectedOption
                            ?.dataset
                            ?.available
                    );


                if (
                    Number.isFinite(available) &&
                    quantity > available
                ) {

                    toast(
                        `Only ${available} units are available`
                    );

                    return;
                }


                await api(

                    `/victims/${
                        session.user.victim_id
                    }/resource`,

                    {

                        method: 'PUT',

                        body:
                            JSON.stringify({

                                resource:
                                    resource,

                                quantity:
                                    quantity

                            })

                    }

                );


                e.target.reset();


                toast(
                    'Resource request submitted'
                );


                loadVictimPortal();

            } catch (error) {

                toast(
                    error.message
                );
            }
        };
}


/* =====================================================
   VICTIM SOS
===================================================== */

async function victimSendSos(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);


        await api(

            `/victims/${
                session.user.victim_id
            }/sos`,

            {

                method: 'PUT',

                body:
                    JSON.stringify(
                        Object.fromEntries(form)
                    )

            }

        );


        closeModal(
            'victimSosModal'
        );


        e.target.reset();


        toast(
            'Emergency SOS sent to command center'
        );


        loadVictimPortal();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   ESCAPE HTML
===================================================== */

function escapeHtml(value) {

    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}


/* =====================================================
   START APPLICATION
===================================================== */

if (session) {
    showApp();
}

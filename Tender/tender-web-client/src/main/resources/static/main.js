function showLoading(element) {
    element.classList.add('loading');
}

function hideLoading(element) {
    element.classList.remove('loading');
}

document.addEventListener('DOMContentLoaded', () => {
    const tables = document.querySelectorAll('table');
    tables.forEach(table => {
        const rows = table.querySelectorAll('tr');
        rows.forEach(row => {
            row.addEventListener('mouseenter', () => {
                row.style.transition = 'background-color 0.3s ease';
            });
        });
    });

    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        const inputs = form.querySelectorAll('input, select');
        inputs.forEach(input => {
            input.addEventListener('input', () => {
                validateInput(input);
            });
        });

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            if (validateForm(form)) {
                const submitButton = form.querySelector('[type="submit"]');
                if (submitButton) {
                    showLoading(submitButton);
                }

                try {
                    await submitForm(form);
                    showSuccess('Form submitted successfully!');
                } catch (error) {
                    showError('An error occurred. Please try again.');
                } finally {
                    if (submitButton) {
                        hideLoading(submitButton);
                    }
                }
            }
        });
    });

    const scrollButton = document.createElement('button');
    scrollButton.innerHTML = '↑';
    scrollButton.className = 'scroll-top';
    scrollButton.style.cssText = `
        position: fixed;
        bottom: 20px;
        right: 20px;
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background-color: #2ecc71;
        color: white;
        border: none;
        cursor: pointer;
        opacity: 0;
        transition: opacity 0.3s ease, transform 0.3s ease;
        box-shadow: 0 2px 5px rgba(0,0,0,0.2);
        z-index: 1000;
    `;

    document.body.appendChild(scrollButton);

    window.addEventListener('scroll', () => {
        if (window.pageYOffset > 300) {
            scrollButton.style.opacity = '1';
            scrollButton.style.transform = 'translateY(0)';
        } else {
            scrollButton.style.opacity = '0';
            scrollButton.style.transform = 'translateY(20px)';
        }
    });

    scrollButton.addEventListener('click', () => {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
});

function validateInput(input) {
    const errorElement = input.nextElementSibling;
    if (!errorElement || !errorElement.classList.contains('error-message')) {
        return;
    }

    if (input.validity.valueMissing) {
        errorElement.textContent = 'This field is required';
    } else if (input.validity.typeMismatch) {
        errorElement.textContent = 'Please enter a valid value';
    } else if (input.validity.patternMismatch) {
        errorElement.textContent = 'Please match the requested format';
    } else {
        errorElement.textContent = '';
    }
}

function validateForm(form) {
    let isValid = true;
    const inputs = form.querySelectorAll('input, select');
    
    inputs.forEach(input => {
        if (!input.validity.valid) {
            isValid = false;
            validateInput(input);
        }
    });

    return isValid;
}

function createToast(message, type) {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 12px 24px;
        background-color: ${type === 'success' ? '#2ecc71' : '#e74c3c'};
        color: white;
        border-radius: 8px;
        box-shadow: 0 4px 8px rgba(0,0,0,0.1);
        z-index: 1000;
        opacity: 0;
        transform: translateY(-20px);
        transition: opacity 0.3s ease, transform 0.3s ease;
    `;

    document.body.appendChild(toast);
    toast.offsetHeight;
    
    toast.style.opacity = '1';
    toast.style.transform = 'translateY(0)';

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(-20px)';
        setTimeout(() => {
            document.body.removeChild(toast);
        }, 300);
    }, 3000);
}

function showSuccess(message) {
    createToast(message, 'success');
}

function showError(message) {
    createToast(message, 'error');
}

async function submitForm(form) {
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    
    const response = await fetch(form.action, {
        method: form.method || 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    });

    if (!response.ok) {
        throw new Error('Form submission failed');
    }

    return response.json();
} 
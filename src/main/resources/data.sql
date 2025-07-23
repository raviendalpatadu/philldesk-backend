-- Initial data for PhillDesk application

-- Insert default roles
INSERT INTO roles (name, description) VALUES 
('ADMIN', 'System administrator with full access'),
('PHARMACIST', 'Pharmacist who can approve prescriptions and manage inventory'),
('CUSTOMER', 'Customer who can upload prescriptions and view order history')
ON CONFLICT (name) DO NOTHING;

-- Sample medicines for testing (only if tables are empty)
INSERT INTO medicines (name, generic_name, manufacturer, category, dosage_form, strength, quantity, unit_price, cost_price, expiry_date, reorder_level, is_prescription_required, description) VALUES 
('Panadol', 'Paracetamol', 'GSK', 'Pain Relief', 'Tablet', '500mg', 100, 25.00, 20.00, '2025-12-31', 20, FALSE, 'Pain and fever relief medication'),
('Amoxil', 'Amoxicillin', 'GSK', 'Antibiotic', 'Capsule', '250mg', 50, 155.50, 120.00, '2025-06-30', 15, TRUE, 'Antibiotic for bacterial infections'),
('Voltaren', 'Diclofenac', 'Novartis', 'Anti-inflammatory', 'Tablet', '50mg', 75, 85.00, 68.00, '2025-09-15', 25, TRUE, 'Non-steroidal anti-inflammatory drug'),
('Piriton', 'Chlorpheniramine', 'GSK', 'Antihistamine', 'Tablet', '4mg', 80, 45.00, 35.00, '2025-08-20', 30, FALSE, 'Antihistamine for allergic reactions'),
('Augmentin', 'Amoxicillin + Clavulanic Acid', 'GSK', 'Antibiotic', 'Tablet', '625mg', 40, 185.00, 150.00, '2025-10-10', 20, TRUE, 'Broad spectrum antibiotic'),
('Omeprazole', 'Omeprazole', 'Local Pharma', 'Antacid', 'Capsule', '20mg', 60, 75.00, 55.00, '2026-01-15', 25, TRUE, 'Proton pump inhibitor for acid reflux'),
('Vitamin C', 'Ascorbic Acid', 'Nature''s Way', 'Vitamin', 'Tablet', '1000mg', 120, 35.00, 25.00, '2026-03-30', 40, FALSE, 'Vitamin C supplement for immunity'),
('Aspirin', 'Acetylsalicylic Acid', 'Bayer', 'Pain Relief', 'Tablet', '300mg', 90, 28.00, 22.00, '2025-11-25', 30, FALSE, 'Pain relief and blood thinner'),
('Metformin', 'Metformin HCl', 'Local Pharma', 'Diabetes', 'Tablet', '500mg', 70, 45.00, 35.00, '2025-07-18', 25, TRUE, 'Medication for type 2 diabetes'),
('Losartan', 'Losartan Potassium', 'Teva', 'Blood Pressure', 'Tablet', '50mg', 55, 95.00, 75.00, '2025-12-05', 20, TRUE, 'ACE inhibitor for high blood pressure');

-- Sample bills for testing payment functionality
-- Note: These will only work if you have sample users and prescriptions already
-- Bill 1: Pending bill for customer testing
INSERT INTO bills (bill_number, customer_id, pharmacist_id, prescription_id, subtotal, discount, tax, total_amount, payment_status, payment_type, notes) VALUES 
('BILL-2025-001', 
 (SELECT id FROM users WHERE username = 'customer' LIMIT 1),
 (SELECT id FROM users WHERE role_id = (SELECT id FROM roles WHERE name = 'PHARMACIST') LIMIT 1),
 NULL,
 150.00, 10.00, 15.60, 155.60, 'PENDING', 'PAY_ON_PICKUP', 'Sample bill for testing payment functionality')
ON CONFLICT (bill_number) DO NOTHING;

-- Bill 2: Another pending bill set for online payment
INSERT INTO bills (bill_number, customer_id, pharmacist_id, prescription_id, subtotal, discount, tax, total_amount, payment_status, payment_type, notes) VALUES 
('BILL-2025-002', 
 (SELECT id FROM users WHERE username = 'customer' LIMIT 1),
 (SELECT id FROM users WHERE role_id = (SELECT id FROM roles WHERE name = 'PHARMACIST') LIMIT 1),
 NULL,
 85.00, 0.00, 8.50, 93.50, 'PENDING', 'ONLINE', 'Online payment test bill')
ON CONFLICT (bill_number) DO NOTHING;

-- Bill 3: Paid bill for history viewing
INSERT INTO bills (bill_number, customer_id, pharmacist_id, prescription_id, subtotal, discount, tax, total_amount, payment_status, payment_method, payment_type, paid_at, notes) VALUES 
('BILL-2025-003', 
 (SELECT id FROM users WHERE username = 'customer' LIMIT 1),
 (SELECT id FROM users WHERE role_id = (SELECT id FROM roles WHERE name = 'PHARMACIST') LIMIT 1),
 NULL,
 75.00, 5.00, 7.50, 77.50, 'PAID', 'ONLINE', 'ONLINE', CURRENT_TIMESTAMP - INTERVAL '2 days', 'Completed online payment')
ON CONFLICT (bill_number) DO NOTHING;

-- Sample bill items for the bills above
INSERT INTO bill_items (bill_id, medicine_id, quantity, unit_price, total_price) VALUES 
((SELECT id FROM bills WHERE bill_number = 'BILL-2025-001'), (SELECT id FROM medicines WHERE name = 'Panadol' LIMIT 1), 2, 25.00, 50.00),
((SELECT id FROM bills WHERE bill_number = 'BILL-2025-001'), (SELECT id FROM medicines WHERE name = 'Amoxil' LIMIT 1), 1, 155.50, 155.50),
((SELECT id FROM bills WHERE bill_number = 'BILL-2025-002'), (SELECT id FROM medicines WHERE name = 'Voltaren' LIMIT 1), 1, 85.00, 85.00),
((SELECT id FROM bills WHERE bill_number = 'BILL-2025-003'), (SELECT id FROM medicines WHERE name = 'Omeprazole' LIMIT 1), 1, 75.00, 75.00)
ON CONFLICT DO NOTHING;

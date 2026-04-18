-- ===========================================
-- Massive Seed for H2 (30 properties + inspections + images)
-- ===========================================

-- 1️⃣ Propriedades (Property Listings)
INSERT INTO property_listings (id, title, description, city, tags) VALUES
('00000000-0000-0000-0000-000000000001', 'Apartamento Central Luxo', 'Cobertura moderna com vista panorâmica', 'São Paulo', '["cobertura","luxo","vista"]'),
('00000000-0000-0000-0000-000000000002', 'Studio Compacto', 'Studio funcional próximo ao metrô', 'São Paulo', '["studio","metrô","moderno"]'),
('00000000-0000-0000-0000-000000000003', 'Casa Jardim Botânico', 'Casa térrea com jardim e churrasqueira', 'Campinas', '["casa","jardim","churrasqueira"]'),
('00000000-0000-0000-0000-000000000004', 'Apartamento Bela Vista', 'Apartamento com varanda e 3 dormitórios', 'Curitiba', '["apartamento","varanda","3 quartos"]'),
('00000000-0000-0000-0000-000000000005', 'Cobertura Atlântica', 'Cobertura com piscina privativa e vista para o mar', 'Florianópolis', '["cobertura","mar","piscina"]'),
('00000000-0000-0000-0000-000000000006', 'Casa Colonial', 'Estilo clássico no bairro histórico', 'Ouro Preto', '["colonial","histórico","patrimônio"]'),
('00000000-0000-0000-0000-000000000007', 'Loft Industrial', 'Ambiente integrado com pé-direito duplo', 'São Paulo', '["loft","industrial","design"]'),
('00000000-0000-0000-0000-000000000008', 'Casa Lago Azul', 'Residência de luxo à beira do lago', 'Brasília', '["luxo","lago","natureza"]'),
('00000000-0000-0000-0000-000000000009', 'Apartamento Vila Olímpia', 'Localização premium com academia', 'São Paulo', '["academia","vila olímpia","premium"]'),
('00000000-0000-0000-0000-000000000010', 'Casa Serra Verde', 'Casa ampla em condomínio fechado', 'Belo Horizonte', '["casa","condomínio","serra"]'),
('00000000-0000-0000-0000-000000000011', 'Casa Beira Rio', 'Casa charmosa com deck e vista para o rio', 'Joinville', '["deck","rio","charmosa"]'),
('00000000-0000-0000-0000-000000000012', 'Apartamento Minimalista', 'Ambiente clean e moderno', 'São Paulo', '["minimalista","design","moderno"]'),
('00000000-0000-0000-0000-000000000013', 'Casa Campo Alegre', 'Casa de campo com pomar e piscina', 'Itu', '["campo","piscina","pomar"]'),
('00000000-0000-0000-0000-000000000014', 'Apartamento Costa Verde', 'Frente mar com sacada gourmet', 'Rio de Janeiro', '["mar","sacada","gourmet"]'),
('00000000-0000-0000-0000-000000000015', 'Casa Alto das Palmeiras', 'Espaço amplo com 5 suítes', 'Campinas', '["suítes","amplo","luxo"]'),
('00000000-0000-0000-0000-000000000016', 'Apartamento Jardim Europa', 'Cobertura duplex com lareira', 'São Paulo', '["cobertura","duplex","lareira"]'),
('00000000-0000-0000-0000-000000000017', 'Casa Solar das Flores', 'Linda vista para as montanhas', 'Petrópolis', '["vista","montanha","flores"]'),
('00000000-0000-0000-0000-000000000018', 'Apartamento Vila Mariana', 'Excelente localização com varanda', 'São Paulo', '["varanda","metrô","localização"]'),
('00000000-0000-0000-0000-000000000019', 'Casa Bosque Encantado', 'Casa rústica no meio da natureza', 'Gramado', '["rústica","natureza","bosque"]'),
('00000000-0000-0000-0000-000000000020', 'Apartamento Horizonte Azul', 'Amplo e bem iluminado', 'Curitiba', '["amplo","iluminado","andar alto"]'),
('00000000-0000-0000-0000-000000000021', 'Casa Jardim Secreto', 'Espaço verde e tranquilo', 'Campinas', '["tranquilo","verde","jardim"]'),
('00000000-0000-0000-0000-000000000022', 'Apartamento Nova Paulista', 'Localização estratégica e moderna', 'São Paulo', '["estratégico","paulista","moderno"]'),
('00000000-0000-0000-0000-000000000023', 'Casa do Sol', 'Ambiente arejado com piscina', 'Ribeirão Preto', '["arejado","piscina","sol"]'),
('00000000-0000-0000-0000-000000000024', 'Apartamento Horizonte Verde', 'Com área de lazer completa', 'Curitiba', '["lazer","verde","completo"]'),
('00000000-0000-0000-0000-000000000025', 'Casa das Palmeiras', 'Casa térrea moderna', 'Campinas', '["moderna","térrea","palmeiras"]'),
('00000000-0000-0000-0000-000000000026', 'Apartamento Bela Colina', 'Vista panorâmica para o vale', 'Belo Horizonte', '["vista","vale","panorâmica"]'),
('00000000-0000-0000-0000-000000000027', 'Casa Alto da Serra', 'Clima serrano e arquitetura rústica', 'Campos do Jordão', '["serra","rústica","clima frio"]'),
('00000000-0000-0000-0000-000000000028', 'Apartamento do Parque', 'Ao lado do parque central', 'Porto Alegre', '["parque","central","verde"]'),
('00000000-0000-0000-0000-000000000029', 'Casa Brisa Mar', 'Casa ampla de frente para o mar', 'Florianópolis', '["mar","frente","ampla"]'),
('00000000-0000-0000-0000-000000000030', 'Apartamento Sol Nascente', 'Ambiente iluminado e aconchegante', 'São Paulo', '["aconchego","luz","sol"]');

-- 2️⃣ Inspeções (uma por propriedade)
INSERT INTO inspections (id, property_id, inspector, notes) VALUES
('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Ricardo Alves', 'Tudo em ordem'),
('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 'Mariana Costa', 'Revisar pintura do teto'),
('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003', 'João Pereira', 'Jardim impecável'),
('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000004', 'Fernanda Lima', 'Porta de varanda desalinhada'),
('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000005', 'Carlos Mendes', 'Piscina precisa de limpeza'),
('10000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000006', 'Amanda Rocha', 'Telhado revisado'),
('10000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000007', 'Tiago Borges', 'Sem observações'),
('10000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000008', 'Laura Campos', 'Sistema de irrigação funcional'),
('10000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000009', 'Pedro Lima', 'Academia nova'),
('10000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000010', 'Sofia Almeida', 'Revisar portão eletrônico'),
('10000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000011', 'Diego Nunes', 'Deck revisado'),
('10000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000012', 'Camila Torres', 'Apartamento impecável'),
('10000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000013', 'Vinícius Souza', 'Piscina ok'),
('10000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000014', 'Ana Clara', 'Vidros limpos'),
('10000000-0000-0000-0000-000000000015', '00000000-0000-0000-0000-000000000015', 'Bruno Rocha', 'Banheiros revisados'),
('10000000-0000-0000-0000-000000000016', '00000000-0000-0000-0000-000000000016', 'Eduardo Lima', 'Lareira testada e ok'),
('10000000-0000-0000-0000-000000000017', '00000000-0000-0000-0000-000000000017', 'Patrícia Alves', 'Janelas precisam de ajuste'),
('10000000-0000-0000-0000-000000000018', '00000000-0000-0000-0000-000000000018', 'Felipe Santos', 'Varanda revisada'),
('10000000-0000-0000-0000-000000000019', '00000000-0000-0000-0000-000000000019', 'Isabela Duarte', 'Pintura nova'),
('10000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000020', 'André Ferreira', 'Tudo conforme'),
('10000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-000000000021', 'Carla Ramos', 'Área externa excelente'),
('10000000-0000-0000-0000-000000000022', '00000000-0000-0000-0000-000000000022', 'Rafael Gomes', 'Localização ideal'),
('10000000-0000-0000-0000-000000000023', '00000000-0000-0000-0000-000000000023', 'Tatiane Barros', 'Piscina com vazamento leve'),
('10000000-0000-0000-0000-000000000024', '00000000-0000-0000-0000-000000000024', 'Lucas Fernandes', 'Sistema elétrico revisado'),
('10000000-0000-0000-0000-000000000025', '00000000-0000-0000-0000-000000000025', 'Letícia Moraes', 'Sem observações'),
('10000000-0000-0000-0000-000000000026', '00000000-0000-0000-0000-000000000026', 'Roberto Dias', 'Vista excelente'),
('10000000-0000-0000-0000-000000000027', '00000000-0000-0000-0000-000000000027', 'Viviane Lopes', 'Ambiente rústico preservado'),
('10000000-0000-0000-0000-000000000028', '00000000-0000-0000-0000-000000000028', 'Bruno Oliveira', 'Parque próximo e seguro'),
('10000000-0000-0000-0000-000000000029', '00000000-0000-0000-0000-000000000029', 'Juliana Reis', 'Pintura externa revisada'),
('10000000-0000-0000-0000-000000000030', '00000000-0000-0000-0000-000000000030', 'Henrique Faria', 'Ambiente aconchegante');

-- 3️⃣ Imagens (3 amostras por propriedade, 90 no total)
INSERT INTO images (id, data, mime, caption, property_id) VALUES
('20000000-0000-0000-0000-000000000001', RANDOM_UUID(), 'image/jpeg', 'Fachada principal', '00000000-0000-0000-0000-000000000001'),
('20000000-0000-0000-0000-000000000002', RANDOM_UUID(), 'image/jpeg', 'Sala de estar', '00000000-0000-0000-0000-000000000001'),
('20000000-0000-0000-0000-000000000003', RANDOM_UUID(), 'image/png', 'Varanda', '00000000-0000-0000-0000-000000000001'),
('20000000-0000-0000-0000-000000000004', RANDOM_UUID(), 'image/jpeg', 'Cozinha moderna', '00000000-0000-0000-0000-000000000002'),
('20000000-0000-0000-0000-000000000005', RANDOM_UUID(), 'image/jpeg', 'Banheiro', '00000000-0000-0000-0000-000000000002');
-- (... continue até 90 imagens, variando captions e mime ...)

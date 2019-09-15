create table userinfo (
    id int not null primary key AUTO_INCREMENT,
    name varchar(50),
    idcard varchar(50),
    sex smallint(1)
);

insert into userinfo(name,idcard,sex)
values
('a','1',0),
('b','2',0),
('c','3',0),
('d','4',0),
('e','5',0),
('f','6',0),
('g','7',0);